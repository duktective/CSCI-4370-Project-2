/**
Copyright (c) 2024 Sami Menik, PhD. All rights reserved.

This is a project developed by Dr. Menik to give the students an opportunity to apply database concepts learned in the class in a real world project. Permission is granted to host a running version of this software and to use images or videos of this work solely for the purpose of demonstrating the work to potential employers. Any form of reproduction, distribution, or transmission of the software's source code, in part or whole, without the prior written consent of the copyright owner, is strictly prohibited.
*/
package uga.menik.csx370.controllers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import uga.menik.csx370.models.ExpandedPost;
import uga.menik.csx370.utility.Utility;

/**
 * Handles /post URL and its sub urls.
 */
@Controller
@RequestMapping("/post")
public class PostController {

    /**
     * This function handles the /post/{postId} URL.
     * This handlers serves the web page for a specific post.
     * Note there is a path variable {postId}.
     * An example URL handled by this function looks like below:
     * http://localhost:8081/post/1
     * The above URL assigns 1 to postId.
     * 
     * See notes from HomeController.java regardig error URL parameter.
     */
    @GetMapping("/{postId}")
    public ModelAndView webpage(@PathVariable("postId") String postId,
            @RequestParam(name = "error", required = false) String error) {
        System.out.println("The user is attempting to view post with id: " + postId);
        // See notes on ModelAndView in BookmarksController.java.
        ModelAndView mv = new ModelAndView("posts_page");

        try {
            java.sql.Connection conn = java.sql.DriverManager.getConnection(
                    "jdbc:mysql://localhost:33306/csx370_mb_platform",
                    "root",
                    "mysqlpass");

            String postSql = """
                    select p.postId, p.content, p.createdAt,
                           u.userId, u.firstName, u.lastName
                    from post p
                    join `user` u on p.userId = u.userId
                    where p.postId = ?
                    """;

            String commentSql = """
                    select c.commentId, c.content, c.createdAt,
                           u.userId, u.firstName, u.lastName
                    from comment c
                    join `user` u on c.userId = u.userId
                    where c.postId = ?
                    order by c.createdAt asc, c.commentId asc
                    """;

            java.sql.PreparedStatement postStmt = conn.prepareStatement(postSql);
            postStmt.setString(1, postId);

            java.sql.ResultSet postRs = postStmt.executeQuery();

            java.util.ArrayList<uga.menik.csx370.models.Comment> comments =
                    new java.util.ArrayList<>();

            uga.menik.csx370.models.User postUser = null;
            String content = "";
            String createdAt = "";

            if (postRs.next()) {
                content = postRs.getString("content");
                createdAt = postRs.getString("createdAt");

                postUser = new uga.menik.csx370.models.User(
                        postRs.getString("userId"),
                        postRs.getString("firstName"),
                        postRs.getString("lastName"));
            }

            java.sql.PreparedStatement commentStmt = conn.prepareStatement(commentSql);
            commentStmt.setString(1, postId);

            java.sql.ResultSet commentRs = commentStmt.executeQuery();

            while (commentRs.next()) {

                uga.menik.csx370.models.User commentUser =
                        new uga.menik.csx370.models.User(
                                commentRs.getString("userId"),
                                commentRs.getString("firstName"),
                                commentRs.getString("lastName"));

                uga.menik.csx370.models.Comment comment =
                        new uga.menik.csx370.models.Comment(
                                commentRs.getString("commentId"),
                                commentRs.getString("content"),
                                commentRs.getString("createdAt"),
                                commentUser);

                comments.add(comment);
            }

            uga.menik.csx370.models.ExpandedPost post =
                    new uga.menik.csx370.models.ExpandedPost(
                            postId,
                            content,
                            createdAt,
                            postUser,
                            0,
                            comments.size(),
                            false,
                            false,
                            comments);

            mv.addObject("posts", java.util.List.of(post));

            conn.close();

        } catch (Exception e) {
            e.printStackTrace();
            mv.addObject("posts", Utility.createSampleExpandedPostWithComments());
        }

        String errorMessage = error;
        mv.addObject("errorMessage", errorMessage);

        return mv;
    }

    /**
     * Handles comments added on posts.
     * See comments on webpage function to see how path variables work here.
     * This function handles form posts.
     * See comments in HomeController.java regarding form submissions.
     */
    @PostMapping("/{postId}/comment")
    public String postComment(@PathVariable("postId") String postId,
            @RequestParam(name = "comment") String comment) {
        System.out.println("The user is attempting add a comment:");
        System.out.println("\tpostId: " + postId);
        System.out.println("\tcomment: " + comment);

        try {

            java.sql.Connection conn = java.sql.DriverManager.getConnection(
                    "jdbc:mysql://localhost:33306/csx370_mb_platform",
                    "root",
                    "mysqlpass");

            String sql = """
                    insert into comment (postId, userId, content)
                    values (?, ?, ?)
                    """;

            java.sql.PreparedStatement pstmt = conn.prepareStatement(sql);

            pstmt.setString(1, postId);
            pstmt.setString(2, "1");
            pstmt.setString(3, comment);

            pstmt.executeUpdate();

            conn.close();

            return "redirect:/post/" + postId;

        } catch (Exception e) {
            e.printStackTrace();
        }

        String message = URLEncoder.encode("Failed to post the comment. Please try again.",
                StandardCharsets.UTF_8);
        return "redirect:/post/" + postId + "?error=" + message;
    }

    /**
     * Handles likes added on posts.
     * See comments on webpage function to see how path variables work here.
     * See comments in PeopleController.java in followUnfollowUser function regarding 
     * get type form submissions and how path variables work.
     */
    @GetMapping("/{postId}/heart/{isAdd}")
    public String addOrRemoveHeart(@PathVariable("postId") String postId,
            @PathVariable("isAdd") Boolean isAdd) {
        System.out.println("The user is attempting add or remove a heart:");
        System.out.println("\tpostId: " + postId);
        System.out.println("\tisAdd: " + isAdd);

        String message = URLEncoder.encode("Failed to (un)like the post. Please try again.",
                StandardCharsets.UTF_8);
        return "redirect:/post/" + postId + "?error=" + message;
    }

    /**
     * Handles bookmarking posts.
     * See comments on webpage function to see how path variables work here.
     * See comments in PeopleController.java in followUnfollowUser function regarding 
     * get type form submissions.
     */
    @GetMapping("/{postId}/bookmark/{isAdd}")
    public String addOrRemoveBookmark(@PathVariable("postId") String postId,
            @PathVariable("isAdd") Boolean isAdd) {
        System.out.println("The user is attempting add or remove a bookmark:");
        System.out.println("\tpostId: " + postId);
        System.out.println("\tisAdd: " + isAdd);

        String message = URLEncoder.encode("Failed to (un)bookmark the post. Please try again.",
                StandardCharsets.UTF_8);
        return "redirect:/post/" + postId + "?error=" + message;
    }

}
