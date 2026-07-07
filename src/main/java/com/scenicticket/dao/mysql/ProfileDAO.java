package com.scenicticket.dao.mysql;

import com.scenicticket.dao.BaseDAO;
import com.scenicticket.exception.DBException;
import com.scenicticket.model.Profile;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class ProfileDAO extends BaseDAO {
    public long create(Profile profile) {
        String sql = "INSERT INTO profiles (user_id, real_name, id_card, address, notes) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindProfile(statement, profile);
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
            }
            throw new DBException("Failed to read generated profile id.");
        } catch (SQLException e) {
            throw new DBException("Failed to create profile.", e);
        }
    }

    public Optional<Profile> findByUserId(long userId) {
        String sql = "SELECT profile_id, user_id, real_name, id_card, address, notes FROM profiles WHERE user_id = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapProfile(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DBException("Failed to find profile by user id.", e);
        }
    }

    public boolean upsert(Profile profile) {
        String sql = """
                INSERT INTO profiles (user_id, real_name, id_card, address, notes)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    real_name = VALUES(real_name),
                    id_card = VALUES(id_card),
                    address = VALUES(address),
                    notes = VALUES(notes)
                """;
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindProfile(statement, profile);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DBException("Failed to upsert profile.", e);
        }
    }

    private void bindProfile(PreparedStatement statement, Profile profile) throws SQLException {
        statement.setLong(1, profile.getUserId());
        statement.setString(2, profile.getRealName());
        statement.setString(3, profile.getIdCard());
        statement.setString(4, profile.getAddress());
        statement.setString(5, profile.getNotes());
    }

    private Profile mapProfile(ResultSet resultSet) throws SQLException {
        Profile profile = new Profile();
        profile.setProfileId(resultSet.getLong("profile_id"));
        profile.setUserId(resultSet.getLong("user_id"));
        profile.setRealName(resultSet.getString("real_name"));
        profile.setIdCard(resultSet.getString("id_card"));
        profile.setAddress(resultSet.getString("address"));
        profile.setNotes(resultSet.getString("notes"));
        return profile;
    }
}
