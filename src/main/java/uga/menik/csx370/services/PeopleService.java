/**
Copyright (c) 2024 Sami Menik, PhD. All rights reserved.

This is a project developed by Dr. Menik to give the students an opportunity to apply database concepts learned in the class in a real world project. Permission is granted to host a running version of this software and to use images or videos of this work solely for the purpose of demonstrating the work to potential employers. Any form of reproduction, distribution, or transmission of the software's source code, in part or whole, without the prior written consent of the copyright owner, is strictly prohibited.
*/
package uga.menik.csx370.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

import org.springframework.stereotype.Service;

import com.mysql.cj.jdbc.MysqlDataSource;

import uga.menik.csx370.models.FollowableUser;
import uga.menik.csx370.models.Post;
import uga.menik.csx370.models.User;

/**
 * This service contains people related functions.
 */
@Service
public class PeopleService {

    private final DataSource dataSource;
    private final UserService userService;

    public PeopleService(DataSource dataSource, UserService userService) {
        this.dataSource = dataSource;
        this.userService = userService;

    }

    /**
     * This function should query and return all users that
     * are followable. The list should not contain the user
     * with id userIdToExclude.
     */
    public List<FollowableUser> getFollowableUsers(String userIdToExclude) throws SQLException {
        // Write an SQL query to find the users that are not the current user.

        // Run the query with a datasource.
        // See UserService.java to see how to inject DataSource instance and
        // use it to run a query.

        // Use the query result to create a list of followable users.
        // See UserService.java to see how to access rows and their attributes
        // from the query result.
        // Check the following createSampleFollowableUserList function to see
        // how to create a list of FollowableUsers.

        // Replace the following line and return the list you created.

        List<FollowableUser> fin = new ArrayList<>();
        String command = """
                SELECT
                    u.userId,
                    u.firstName,
                    u.lastName,
                    f.followerId
                FROM user u
                LEFT JOIN follow f
                    ON u.userId = f.followeeId
                    AND f.followerId = ?
                WHERE u.userId <> ?
                """;
        try (Connection con = dataSource.getConnection();
                PreparedStatement stm = con.prepareStatement(command)) {
            stm.setString(1, userIdToExclude);
            stm.setString(2, userIdToExclude);
            try (ResultSet rs = stm.executeQuery()) {
                while (rs.next()) {
                    String userId = rs.getString("userId");
                    String firstName = rs.getString("firstName");
                    String lastName = rs.getString("lastName");
                    boolean isFollowed = rs.getString("followerId") != null;
                    String lastActiveDate = "NA";

                    fin.add(new FollowableUser(userId, firstName, lastName, isFollowed, lastActiveDate));
                }
            }
            return fin;
        }

    }

    public void followUser(String followerId, String followeeId) throws SQLException {

        String sql = """
                INSERT INTO follow (followerId, followeeId)
                VALUES (?, ?)
                """;

        try (Connection conn = dataSource.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, followerId);
            pstmt.setString(2, followeeId);

            pstmt.executeUpdate();
        }
    }

    public void unfollowUser(String followerId, String followeeId) throws SQLException {

        String sql = """
                DELETE FROM follow
                WHERE followerId = ? AND followeeId = ?
                """;

        try (Connection conn = dataSource.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, followerId);
            pstmt.setString(2, followeeId);

            pstmt.executeUpdate();
        }
    }

    public List<Post> getPostsByUser(String userId) throws SQLException {
        List<Post> fin = new ArrayList<>();
        String command = """
                SELECT p.postId, p.content, p.createdAt,
                       u.userId, u.firstName, u.lastName
                FROM post p
                JOIN user u ON p.userId = u.userId
                WHERE p.userId = ?
                ORDER BY p.createdAt DESC
                """;

        try (Connection conn = dataSource.getConnection();
                PreparedStatement stm = conn.prepareStatement(command)) {

            stm.setString(1, userId);

            try (ResultSet rs = stm.executeQuery()) {

                while (rs.next()) {

                    String postId = rs.getString("postId");
                    String content = rs.getString("content");
                    String createdAt = rs.getString("createdAt");
                    String uId = rs.getString("userId");
                    String firstName = rs.getString("firstName");
                    String lastName = rs.getString("lastName");
                    User user = new User(uId, firstName, lastName);
                    int likeCount = 0;
                    int commentCount = 0;
                    boolean isLiked = false;
                    boolean isBookmarked = false;

                    fin.add(new Post(postId, content, createdAt, user, likeCount, commentCount, isLiked, isBookmarked));
                }
            }
        }

        return fin;
    }
}
