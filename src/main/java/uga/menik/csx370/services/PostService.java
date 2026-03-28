package uga.menik.csx370.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.stereotype.Service;

import uga.menik.csx370.models.Post;
import uga.menik.csx370.models.User;

@Service
public class PostService {

    private final DataSource dataSource;
    private final UserService userService;

    public PostService(DataSource dataSource, UserService userService) {
        this.dataSource = dataSource;
        this.userService = userService;
    }

    private List<String> parseHashtag(String content) {
        List<String> hashtags = new ArrayList<>();
        String[] words = content.split("\\s+");
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (word.startsWith("#") && word.length() > 1) {
                String hashtag = word.substring(1).toLowerCase();
                hashtags.add(hashtag);
            }
        }
        return hashtags;
    }

    public void createNewPost(String content) throws Exception {

        if (!userService.isAuthenticated()) {
            throw new Exception("Login needed to post.");
        }
    
        if (content == null || content.trim().isEmpty()) {
            throw new Exception("Empty posts are not allowed.");
        }
    
        String userId = userService.getLoggedInUser().getUserId();
    
        String sql = """
                insert into post (userId, content) 
                values (?, ?)
                """;
    
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
    
            stmt.setString(1, userId);
            stmt.setString(2, content);
    
            stmt.executeUpdate();
    
            ResultSet rs = stmt.getGeneratedKeys();
    
            int postId;
            if (rs.next()) {
                postId = rs.getInt(1);
            } else {
                throw new Exception("Couldn't get the postId");
            }
    
            List<String> tags = parseHashtag(content);
    
            for (int i = 0; i < tags.size(); i++) {
                String tag = tags.get(i);

                String addHashTagSql = """
                            insert ignore into hashtag (tag) 
                            values (?)
                            """;

                try (PreparedStatement addHashTagSqlStmt = conn.prepareStatement(addHashTagSql)) {
                    addHashTagSqlStmt.setString(1, tag);
                    addHashTagSqlStmt.executeUpdate();
                }
    
                int hashtagId;
                String getHashTagSql = """
                            select hashtagId 
                            from hashtag where tag = ?
                            """;
                try (PreparedStatement getHashTagStmt = conn.prepareStatement(getHashTagSql)) {
                    getHashTagStmt.setString(1, tag);
                    ResultSet tagRs = getHashTagStmt.executeQuery();
    
                    if (tagRs.next()) {
                        hashtagId = tagRs.getInt("hashtagId");
                    } else {
                        throw new Exception("Failed to retrieve hashtagId");
                    }
                }
    
                String connectSql = """
                            insert into postHashtag (postId, hashtagId) 
                            values (?, ?)
                            """;
                try (PreparedStatement connectStmt = conn.prepareStatement(connectSql)) {
                    connectStmt.setInt(1, postId);
                    connectStmt.setInt(2, hashtagId);
                    connectStmt.executeUpdate();
                }
            }
        }
    }

    public List<Post> retrievePosts() throws Exception {
        List<Post> posts = new ArrayList<>();

        if (!userService.isAuthenticated()) {
            return posts;
        }

        String loggedInUserId = userService.getLoggedInUser().getUserId();
        String sql = """
            select p.postId, p.content, 
                   date_format(p.createdAt, '%b %d, %Y %h:%i %p') as createdAt,
                   u.userId, u.firstName, u.lastName,
            (select count(*) from postLike pl where pl.postId = p.postId) as heartsCount,
            (select count(*) from comment c where c.postId = p.postId) as commentsCount,
            exists(
                select 1 from postLike pl2
                where pl2.postId = p.postId and pl2.userId = ?
            ) as isHearted,
            exists(
                select 1 from bookmark b
                where b.postId = p.postId and b.userId = ?
            ) as isBookmarked
            from post p
            join user u on p.userId = u.userId
            order by p.createdAt desc
        """;

     
        try (Connection conn = dataSource.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {
   
            stmt.setString(1, loggedInUserId);
            stmt.setString(2, loggedInUserId);
   
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {

                String postId = rs.getString("postId");
                String content = rs.getString("content");
                String createdAt = rs.getString("createdAt");

                String userId = rs.getString("userId");
                String firstName = rs.getString("firstName");
                String lastName = rs.getString("lastName");

                User user = new User(userId, firstName, lastName);

                int heartsCount = rs.getInt("heartsCount");
                int commentsCount = rs.getInt("commentsCount");
                boolean isHearted = rs.getBoolean("isHearted");
                boolean isBookmarked = rs.getBoolean("isBookmarked");

                posts.add(new Post(
                        postId,
                        content,
                        createdAt,
                        user,
                        heartsCount,
                        commentsCount,
                        isHearted,
                        isBookmarked
                ));
            }
        }
    }
        return posts;
    }


        
        public List<Post> searchForHashtag(List<String> tags) throws Exception {
            List<Post> posts = new ArrayList<>();
    
            if (tags == null || tags.isEmpty()) {
                return posts;
            }
    
            StringBuilder space = new StringBuilder();
            for (int i = 0; i < tags.size(); i++) {
                if (i > 0) {
                    space.append(", "); 
                }
                space.append("?");
            }

            String sql =
            "select p.postId, p.content, " + 
            "date_format(p.createdAt, '%b %d, %Y %h:%i %p') as createdAt, " +
            "u.userId, u.firstName, u.lastName, " +
            "(select count(*) from postLike pl where pl.postId = p.postId) as heartsCount, " +
            "(select count(*) from comment c where c.postId = p.postId) as commentsCount, " +
            "exists(select 1 from postLike pl2 where pl2.postId = p.postId and pl2.userId = ?) as isHearted, " +
            "exists(select 1 from bookmark b where b.postId = p.postId and b.userId = ?) as isBookmarked " +
            "from post p " +
            "join user u on p.userId = u.userId " +
            "join postHashtag ph on p.postId = ph.postId " +
            "join hashtag h on ph.hashtagId = h.hashtagId " +
            "where h.tag in (" + space.toString() + ") " +
            "group by p.postId " +
            "having count(distinct h.tag) = ? " +
            "order by p.createdAt desc";

            try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            int i = 1;
            stmt.setString(i++, userService.getLoggedInUser().getUserId());
            stmt.setString(i++, userService.getLoggedInUser().getUserId());

            for (int k = 0; k < tags.size(); k++) {
                stmt.setString(i++, tags.get(k));
            }

            stmt.setInt(i, tags.size());

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                User user = new User(
                        rs.getString("userId"),
                        rs.getString("firstName"),
                        rs.getString("lastName")
                );

                posts.add(new Post(
                        rs.getString("postId"),
                        rs.getString("content"),
                        rs.getString("createdAt"),
                        user,
                        rs.getInt("heartsCount"),
                        rs.getInt("commentsCount"),
                        rs.getBoolean("isHearted"),
                        rs.getBoolean("isBookmarked")
                ));
            }
        }

        return posts;
    }
}
