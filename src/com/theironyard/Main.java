package com.theironyard;

import spark.ModelAndView;
import spark.Session;
import spark.Spark;
import spark.template.mustache.MustacheTemplateEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.SplittableRandom;
import java.util.StringJoiner;

public class Main {

    static HashMap<String, User> users = new HashMap<>();
    static ArrayList<Message> messages = new ArrayList<>();


    public static void main(String[] args) {
	    addTestUsers();
        addTestMessages();

        Spark.init();
        Spark.get(
                "/",
                ((request, response) ->  {
                    Session session = request.session();
                    String userName = session.attribute("userName");


                    String replyId = request.queryParams("replyId");
                    int replyIdNum = -1;
                    if (replyId != null) {
                        replyIdNum = Integer.valueOf(replyId);
                    }


                    HashMap m = new HashMap();
                    ArrayList<Message> threads = new ArrayList<>();
                    for (Message message : messages) { //Filter
                        if (message.replyId == replyIdNum) {
                            threads.add(message);
                        }
                    }
                    m.put("messages", threads);
                    m.put("userName", userName);
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
    }

    static void addTestUsers() {
        users.put("Alice", new User("Alice", ""));
        users.put("Bob", new User("Bob", ""));
        users.put("Charlie", new User("Charlie", ""));

    }
    static void addTestMessages() {
        messages.add(new Message (0, -1, "Alice", "Hello world!"));
        messages.add(new Message(1, -1, "Bob", "This is another thread!"));
        messages.add(new Message(2, 0, "Charlie", "Cool thread, Alice."));
        messages.add(new Message(3, 2, "Alice", "Thanks"));

    }
}
