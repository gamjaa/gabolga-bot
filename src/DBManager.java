import java.sql.*;

/**
 * Created by jeong on 2017-06-03.
 */
public class DBManager {
    String driver        = "org.mariadb.jdbc.Driver";
    String url           = "jdbc:mariadb://localhost:3306/gabolga";
    String uId           = Config.DB_ID;
    String uPwd          = Config.DB_PW;

    Connection con;
    PreparedStatement pstmt;
    ResultSet rs;

    public DBManager() {
        try {
            Class.forName(driver);
            con = DriverManager.getConnection(url, uId, uPwd);

            if( con != null ){ System.out.println("Database Connect Success"); }

        } catch (ClassNotFoundException e) { System.out.println("Driver Load Fail ***");    }
        catch (SQLException e) { System.out.println("Database Connect Fail ***"); }
    }

    public boolean userCheck(long user) {
        String sql = "SELECT user_id FROM users WHERE user_id = "+user;
        try {
            pstmt                = con.prepareStatement(sql);
            rs                   = pstmt.executeQuery();

            if(rs.next()) {
                return true;
            } else {
                return false;
            }
        } catch (SQLException e) { System.out.println("Select Query Execute Fail ***"); }

        return true;
    }

    public boolean tweetCheck(long tweet) {
        String sql = "SELECT tweet_id FROM tweet WHERE tweet_id = "+tweet;
        try {
            pstmt                = con.prepareStatement(sql);
            rs                   = pstmt.executeQuery();

            if(rs.next()) {
                return true;
            } else {
                return false;
            }
        } catch (SQLException e) { System.out.println("Select Query Execute Fail ***"); }

        return true;
    }

    public long dmCheck() {
        String sql = "SELECT max(dm_id) FROM dm";
        try {
            pstmt                = con.prepareStatement(sql);
            rs                   = pstmt.executeQuery();

            if(rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) { System.out.println("Select Query Execute Fail ***"); }
        return -1;
    }

    public void insert(long user, long tweet) {
        String sql = "INSERT INTO my_map (user_id, tweet_id) VALUES ("+user+", "+tweet+")";
        try {
            pstmt                = con.prepareStatement(sql);
            pstmt.executeUpdate();
        } catch (SQLException e) { System.out.println("Insert Query Execute Fail: "+e.toString()+" ***"); }
    }

    public void insertDM(long dm, long user, int isTweet) {
        String sql = "INSERT INTO dm (dm_id, user_id, isTweet) VALUES ("+dm+", "+user+", "+isTweet+")";
        try {
            pstmt                = con.prepareStatement(sql);
            pstmt.executeUpdate();
        } catch (SQLException e) { System.out.println("Insert Query Execute Fail: "+e.toString()+" ***"); }
    }
}