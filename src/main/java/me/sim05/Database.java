package me.sim05;

import org.json.JSONObject;

import java.sql.*;
import java.util.UUID;

public class Database {
    private static Database database_instance = null;
    Connection connection;

    //region Class (Singleton)
    private Database() throws SQLException, ClassNotFoundException {
        String jdbcUrl = "jdbc:postgresql://aws-0-eu-central-1.pooler.supabase.com:6543/postgres";
        String username = "postgres.shuiasyyukoohbvzecyu";
        String password = "SimList_200";

        Class.forName("org.postgresql.Driver");

        connection = DriverManager.getConnection(jdbcUrl, username, password);
    }

    public static synchronized Database getInstance() throws SQLException, ClassNotFoundException {
        if (database_instance == null)
            database_instance = new Database();

        return database_instance;
    }
    //endregion

    //region User
    public boolean usernameExists(String username) throws SQLException {
        String query = "select * from account where username = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, username);
        ResultSet resultSet = statement.executeQuery();
        return resultSet.next();
    }

    public void insertUser(String username, String password) throws SQLException {
        if (usernameExists(username)) {
            throw new RuntimeException("Username already exists");
        }

        String query = "INSERT INTO account (username, password) VALUES (?, ?)";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, username);
        statement.setString(2, password);
        statement.executeUpdate();
    }

    public Integer deleteUser(UUID id) throws SQLException {
        String query = "DELETE FROM account WHERE id = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setObject(1, id);
        return statement.executeUpdate();
    }

    public UUID getAccountIdByUsername(String username) throws SQLException {
        String query = "SELECT id FROM account WHERE username = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, username);
        ResultSet resultSet = statement.executeQuery();
        if (resultSet.next()) {
            return resultSet.getObject("id", UUID.class);
        }
        return null;
    }

    public UUID getAccountIdBySessionId(String sessionId) throws SQLException {
        String query = "SELECT account_id FROM session WHERE id = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setObject(1, UUID.fromString(sessionId));
        ResultSet resultSet = statement.executeQuery();
        if (resultSet.next()) {
            return resultSet.getObject("account_id", UUID.class);
        }
        return null;
    }

    public String getPassword(String username) throws SQLException {
        String query = "SELECT password FROM account WHERE username = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, username);
        ResultSet resultSet = statement.executeQuery();
        if (resultSet.next()) {
            return resultSet.getString("password");
        }
        return null;
    }
    //endregion

    //region Session / Authentication
    public UUID getSession(UUID account_id, String ip_address) throws SQLException {
        String query = "SELECT id FROM session WHERE account_id = ? AND ip_address = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setObject(1, account_id);
        statement.setString(2, ip_address);
        ResultSet resultSet = statement.executeQuery();
        if (resultSet.next()) {
            return resultSet.getObject("id", UUID.class);
        }
        return null;
    }

    public UUID createSession(UUID account_id, String ip_address) throws SQLException {
        String query = "INSERT INTO session (account_id, ip_address) VALUES (?, ?)";

        PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        statement.setObject(1, account_id);
        statement.setObject(2, ip_address);

        int affectedRows = statement.executeUpdate();

        if (affectedRows == 0) {
            throw new SQLException("Creating user failed, no rows affected.");
        }

        try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
            if (generatedKeys.next()) {
                return generatedKeys.getObject(1, UUID.class);
            }
            else {
                throw new SQLException("Creating user failed, no ID obtained.");
            }
        }
    }
    //endregion

    //region Household
    public void createHousehold(String name, UUID account_id) throws SQLException {
        String query = "INSERT INTO household (name) VALUES (?)";
        PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        statement.setString(1, name);
        int affectedRows = statement.executeUpdate();

        if (affectedRows == 0) {
            throw new SQLException("Creating household failed, no rows affected.");
        }

        int householdId;
        try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
            if (generatedKeys.next()) {
                householdId = generatedKeys.getInt("id");
            }
            else {
                throw new SQLException("Creating user failed, no ID obtained.");
            }
        }

        createAccountHouseholdConnection(account_id, householdId, "admin");
    }

    public void createAccountHouseholdConnection(UUID account_id, Integer household_id, String permission) throws SQLException {
        String query = "INSERT INTO account_to_household (account_id, household_id, permission) VALUES (?, ?, ?)";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setObject(1, account_id);
        statement.setInt(2, household_id);
        statement.setString(3, permission);
        statement.executeUpdate();
    }

    public boolean checkHouseholdMember(UUID accountId, Integer householdId) throws SQLException {
        String query = "select * from account_to_household where account_id = ? and household_id = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setObject(1, accountId);
        statement.setInt(2, householdId);
        ResultSet resultSet = statement.executeQuery();
        return resultSet.next();
    }

    private boolean householdAuthorized(Integer householdId, UUID accountId) throws SQLException {
        String query = "select permission from account_to_household where account_id = ? and household_id = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setObject(1, accountId);
        statement.setInt(2, householdId);
        ResultSet resultSet = statement.executeQuery();

        if (accountId == Utils.masterId) return true;

        if (resultSet.next()) {
            return resultSet.getString("permission").equals("admin");
        }
        return false;
    }

    public void deleteHousehold(Integer id, UUID account_id) throws SQLException {
        boolean authorized = householdAuthorized(id, account_id);
        if (!authorized) {
            throw new RuntimeException("You are not an admin in this household");
        }

        String query = "DELETE FROM household WHERE id = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, id);
        statement.executeUpdate();
    }

    public void updateHousehold(Integer householdId, String newName, UUID accountId) throws SQLException {
        boolean authorized = householdAuthorized(householdId, accountId);
        if (!authorized) {
            throw new RuntimeException("You are not an admin in this household");
        }

        String query = "UPDATE household SET name = ? WHERE id = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, newName);
        statement.setInt(2, householdId);
        statement.executeUpdate();
    }

    public boolean checkHouseholdCode(String household_code) throws SQLException {
        String query = "SELECT id FROM household WHERE code = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, household_code);
        ResultSet resultSet = statement.executeQuery();
        return !resultSet.next();
    }

    public String createHouseholdCode(UUID account_id, Integer household_id) throws SQLException {
        boolean authorized = householdAuthorized(household_id, account_id);
        if (!authorized) {
            throw new RuntimeException("You are not an admin in this household");
        }

        String query = "UPDATE household SET code = ? WHERE id = ?";
        PreparedStatement statement = connection.prepareStatement(query);

        String code;
        do {
            code = Utils.randomString(10);
        } while (!checkHouseholdCode(code));

        statement.setString(1, code);
        statement.setInt(2, household_id);
        statement.executeUpdate();

        return code;
    }

    public void joinHousehold(UUID account_id, String household_code) throws SQLException {
        String query = "SELECT id FROM household WHERE code = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, household_code);
        ResultSet resultSet = statement.executeQuery();
        if (!resultSet.next()) {
            throw new RuntimeException("Household not found");
        }
        Integer household_id = resultSet.getInt("id");

        createAccountHouseholdConnection(account_id, household_id, "member");
    }

    public void setMemberPermission(Integer householdId, UUID accountId, String permission, UUID otherUser) throws SQLException {
        boolean authorized = householdAuthorized(householdId, accountId);
        if (!authorized) {
            throw new RuntimeException("You are not an admin in this household");
        }

        String query = "UPDATE account_to_household SET permission = ? WHERE account_id = ? AND household_id = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, permission);
        statement.setObject(2, otherUser);
        statement.setInt(3, householdId);
        statement.executeUpdate();

        if (checkHouseholdAdmins(householdId)) return;

        selectNewAdmin(householdId);
    }

    public boolean checkHouseholdAdmins(Integer householdId) throws SQLException {
        String query = "select account_id from account_to_household where household_id = ? and permission = 'admin'";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, householdId);
        ResultSet resultSet = statement.executeQuery();

        return resultSet.next();
    }

    private void selectNewAdmin(Integer householdId) throws SQLException {
        String query = "select account_id from account_to_household where household_id = ? and permission = 'member' limit 1";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, householdId);
        ResultSet resultSet = statement.executeQuery();
        if (!resultSet.next()) {
            deleteHousehold(householdId, Utils.masterId);
            return;
        }

        setMemberPermission(householdId, Utils.masterId, "admin", resultSet.getObject("account_id", UUID.class));
    }

    public void leaveHousehold(UUID accountId, Integer householdId) throws SQLException {
        String query = "delete from account_to_household where account_id = ? and household_id = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setObject(1, accountId);
        statement.setInt(2, householdId);
        statement.executeUpdate();

        if (checkHouseholdAdmins(householdId)) return;

        selectNewAdmin(householdId);
    }
    //endregion

    //region Items
    public JSONObject[] getItems(Integer householdId, UUID accountId) throws SQLException {
        boolean authorized = checkHouseholdMember(accountId, householdId);
        if (!authorized) {
            throw new RuntimeException("You are not a member in this household");
        }

        String query = "SELECT * FROM items WHERE household_id = ?";
        PreparedStatement statement = connection.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY);
        statement.setInt(1, householdId);
        ResultSet resultSet = statement.executeQuery();

        resultSet.last();
        JSONObject[] items = new JSONObject[resultSet.getRow()];
        resultSet.beforeFirst();

        int i = 0;
        while (resultSet.next()) {
            JSONObject item = new JSONObject();
            item.put("name", resultSet.getString("name"));
            item.put("quantity", resultSet.getFloat("quantity"));
            item.put("measurement", resultSet.getString("measurement"));
            items[i] = item;
            i++;
        }

        return items;
    }

    public void addItem(Integer householdId, JSONObject item, UUID accountId) throws SQLException {
        boolean authorized = checkHouseholdMember(accountId, householdId);
        if (!authorized) {
            throw new RuntimeException("You are not a member in this household");
        }

        String name = item.getString("name");
        float quantity = item.getFloat("quantity");
        String measurement = item.getString("measurement");

        String query = "INSERT INTO items (household_id, name, quantity, measurement) VALUES (?, ?, ?, ?)";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, householdId);
        statement.setString(2, name);
        statement.setFloat(3, quantity);
        statement.setString(4, measurement);
        statement.executeUpdate();
    }

    public void editItem(Integer householdId, JSONObject item, UUID accountId) throws SQLException {
        boolean authorized = checkHouseholdMember(accountId, householdId);
        if (!authorized) {
            throw new RuntimeException("You are not a member in this household");
        }

        String name = item.getString("name");
        float quantity = item.getFloat("quantity");

        String query = "UPDATE items SET quantity = ? WHERE household_id = ? AND name = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setFloat(1, quantity);
        statement.setInt(2, householdId);
        statement.setString(3, name);
        statement.executeUpdate();
    }

    public void removeItem(Integer householdId, String itemName, UUID accountId) throws SQLException {
        boolean authorized = checkHouseholdMember(accountId, householdId);
        if (!authorized) {
            throw new RuntimeException("You are not a member in this household");
        }

        String query = "DELETE FROM items WHERE household_id = ? AND name = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, householdId);
        statement.setString(2, itemName);
        statement.executeUpdate();
    }
    //endregion
}
