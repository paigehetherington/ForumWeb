package com.theironyard;

import com.sun.xml.internal.xsom.impl.util.SchemaTreeTraverser;
import spark.ModelAndView;
import spark.Session;
import spark.Spark;
import spark.template.mustache.MustacheTemplateEngine;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.SplittableRandom;
import java.util.StringJoiner;

public class Main {

//    static HashMap<String, User> users = new HashMap<>();
//    static ArrayList<Message> messages = new ArrayList<>();

    public static void createTables(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS users (id IDENTITY, name VARCHAR, password VARCHAR)");
        stmt.execute("CREATE TABLE IF NOT EXISTS messages (id IDENTITY, user_id INT, reply_id INT, text VARCHAR)");


    }

    public static void insertUser (Connection conn, String name, String password) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO users VALUES (NULL, ?, ?)");
        stmt.setString(1, name);
        stmt.setString(2, password);
        stmt.execute();
    }

    public static User selectUser (Connection conn, String name) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE name = ?");
        stmt.setString(1, name);
        ResultSet results =stmt.executeQuery();
        if (results.next()) { //if can find, returns user object
            int id = results.getInt("id");
            String password = results.getString("password");
            return new User(id, name, password);
        }
        return null; //if can't find returns null
    }

    public static void insertMessage(Connection conn, int userId, int replyId, String text) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO messages VALUES (NULL, ?, ?, ?)");
        stmt.setInt(1, userId);
        stmt.setInt(2, replyId);
        stmt.setString(3, text);
        stmt.execute();

    }

    public static Message selectMessage(Connection conn, int id) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM messages INNER JOIN users ON messages.user_id = users.id WHERE messages.id = ?");
        stmt.setInt(1, id);
        ResultSet results = stmt.executeQuery();
        if (results.next()) {
            int replyId = results.getInt("messages.reply_id");
            String name = results.getString("users.name");
            String text = results.getString("messages.text");
            return  new Message(id, replyId, name, text);
        }
        return null;
    }

    public static ArrayList<Message> selectReplies(Connection conn, int replyId) throws SQLException {
        ArrayList<Message> messages = new ArrayList<>();
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM messages INNER JOIN users ON messages.user_id = users.id WHERE messages.reply_id = ?");
        stmt.setInt(1, replyId);
        ResultSet results = stmt.executeQuery();
        while (results.next()) {
            int id = results.getInt("messages.id");
            String name = results.getString("users.name");
            String text = results.getString("messages.text");
            Message message = new Message(id, replyId, name, text);
            messages.add(message);

        }
        return messages; //returns AL of message objects
    }


    public static void main(String[] args) throws SQLException {

        Connection conn = DriverManager.getConnection("jdbc:h2:./main");
        createTables(conn);

//        addTestUsers();
//        addTestMessages();

        Spark.init();
        Spark.get(
                "/",
                ((request, response) ->  {
                    Session session = request.session();
                    String userName = session.attribute("userName");


                    String replyId = request.queryParams("replyId"); //sending parameter to get route
                    int replyIdNum = -1;
                    if (replyId != null) {
                        replyIdNum = Integer.valueOf(replyId);
                    }


                    HashMap m = new HashMap();
                    ArrayList<Message> threads = selectReplies(conn, replyIdNum); //replace with method
//                    for (Message message : messages) { //Filter
//                        if (message.replyId == replyIdNum) {
//                            threads.add(message);
//                        }
                    //}
                    m.put("messages", threads);
                    m.put("userName", userName);
                    m.put("replyId", replyIdNum); //reply id ofpage itself so can access in mustache
                    return new ModelAndView(m, "home.html");

                }),
                new MustacheTemplateEngine()
        );
        Spark.post(
                "/login",
                ((request, response) ->  {
                    String userName = request.queryParams("loginName");
                    if (userName == null) {
                        throw new Exception("Login name not found.");
                    }

                    User user = selectUser(conn, userName);
                    if (user == null) {
                        insertUser(conn, userName, "");
//                        user = new User(userName, "");
//                        users.put(userName, user);
                    }
                    Session session = request.session();
                    session.attribute("userName", userName);
                    response.redirect("/");
                    return "";
                })
        );
        Spark.post(
                "/logout",
                ((request, response) ->  {
                    Session session = request.session();
                    session.invalidate();
                    response.redirect("/");
                    return "";
                })
        );

        Spark.post(
                "/create-message",
                ((request, response) ->  {
                    Session session = request.session();
                    String userName = session.attribute("userName");
                    if (userName == null) {
                        throw new Exception("Not logged in.");
                    }

                    String text = request.queryParams("messageText");
                    String replyId = request.queryParams("replyId");
                    if (text == null || replyId == null) {
                        throw new Exception("Didn't get necessary query parameters.");
                    }
                    int replyIdNum = Integer.valueOf(replyId);

                    User user = selectUser(conn, userName); //query user
                    insertMessage(conn,user.id, replyIdNum, text ); //insert message with correct userid

//                    Message m = new Message(messages.size(), replyIdNum, userName, text); //message size creates id and gets next available number
//                    messages.add(m);

                    response.redirect(request.headers("Referer")); //referrer states what url it came from
                    return "";

                })
        );
    }

//    static void addTestUsers() {
//        users.put("Alice", new User("Alice", ""));
//        users.put("Bob", new User("Bob", ""));
//        users.put("Charlie", new User("Charlie", ""));
//
//    }
//    static void addTestMessages() {
//        messages.add(new Message (0, -1, "Alice", "Hello world!"));
//        messages.add(new Message(1, -1, "Bob", "This is another thread!"));
//        messages.add(new Message(2, 0, "Charlie", "Cool thread, Alice."));
//        messages.add(new Message(3, 2, "Alice", "Thanks"));
//
//    }
}
