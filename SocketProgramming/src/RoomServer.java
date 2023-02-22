// Author: Mustafa Yanar, Emir Said Haliloğlu, Yiğit Göksel
// Date: 12/12/2020
// Computer Network Project : Room Server

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class RoomServer {
    private static boolean isBetween(String x1, String y1, String z1) {
        int x = Integer.parseInt(x1);
        int y = Integer.parseInt(y1);
        int z = Integer.parseInt(z1);
        return x >= y && x <= y + z;
    }

    private static boolean isBetweenWithDuration(String x1, String x2, String y1, String z1) {
        int x = Integer.parseInt(x1) + Integer.parseInt(x2);
        int y = Integer.parseInt(y1);
        int z = Integer.parseInt(z1);
        return x >= y && x <= y + z;
    }

    enum Days {
        Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday
    }

    public static String getDay(String day) {
        int i = 0;
        i = Integer.parseInt(day);
        switch (i) {
            case 1:
                return Days.Monday.toString();
            case 2:
                return Days.Tuesday.toString();
            case 3:
                return Days.Wednesday.toString();
            case 4:
                return Days.Thursday.toString();
            case 5:
                return Days.Friday.toString();
            case 6:
                return Days.Saturday.toString();
            case 7:
                return Days.Sunday.toString();
            default:
                return "Invalid day";
        }
    }

    public static void main(String[] args) throws Exception {
        // Set the port number
        int port = 8080;

        // Create a ServerSocket to listen for client connections
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Listening for connections on port " + port);

        while (true) {
            // Accept a client connection
            Socket clientSocket = serverSocket.accept();
            System.out.println("Connection established");

            // Get the input and output streams for reading and writing data to the client
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

            // Read the HTTP request from the client
            String request = in.readLine();
            System.out.println("Request: " + request);

            // Split the request into its individual components
            String method, path, httpVersion;
            try {
                String[] requestParts = request.split(" ");
                method = requestParts[0];
                path = requestParts[1];
                httpVersion = requestParts[2];
            } catch (Exception e) {
                continue;
            }

            // Print out the request details
            System.out.println("Received request:");
            System.out.println("Method: " + method);
            System.out.println("Path: " + path);
            System.out.println("HTTP Version: " + httpVersion);

            // Parse the endpoint path to determine which action to take
            String endpoint = "";
            try {
                endpoint = path.split("/")[1];
                endpoint = endpoint.substring(endpoint.indexOf("/") + 1);
                endpoint = endpoint.substring(0, endpoint.indexOf("?"));
            } catch (Exception e) {
                continue;
            }

            // Parse the query string to get the request parameters
            String query = "";
            int queryIndex = path.indexOf('?');
            if (queryIndex >= 0) {
                query = path.substring(queryIndex + 1);
            }
            String[] params = query.split("&");

            // Connect to the MongoDB database
            ConnectionString connectionString = new ConnectionString(
                    "mongodb+srv://mustafayanar:4431082@cluster0.mzkjiuf.mongodb.net/?retryWrites=true&w=majority");
            System.out.println("Connected to the connectionString successfully");
            MongoClientSettings settings = MongoClientSettings.builder().applyConnectionString(connectionString)
                    .build();
            System.out.println("Connected to the settings successfully");
            MongoClient mongoClient = MongoClients.create(settings);
            MongoDatabase database = mongoClient.getDatabase("network");
            System.out.println("Connected to the MongoDatabase successfully");
            // add enpoint for adding a room
            if (endpoint.equals("add")) {
                String name = "";
                // Parse the request parameters
                for (String param : params) {
                    String[] pair = param.split("=");
                    if (pair[0].equals("name")) {
                        name = pair[1];
                        break;
                    }
                }
                // Add the room to the database
                MongoCollection<Document> collection = database.getCollection("room");
                Document document = new Document("name", name);
                System.out.println("Document inserted successfully");
                int statusCode = 500;
                String response = "Received request to add room: " + name;
                // Check if the room already exists
                if (collection.find(document).first() != null) {
                    System.out.println("Document found successfully");
                    statusCode = 403;
                    response = "Room already exists";
                } else { // If the room does not exist, add it to the database
                    System.out.println("Document not found");
                    statusCode = 200;
                    System.out.println("Document created successfully");
                    collection.insertOne(document);
                    response = "Received request to add room: " + name;
                }
                // Send the response to the client
                out.println("HTTP/1.1 " + statusCode);
                out.println("Content-Type: text/html");
                out.println("Content-Length: " + response.length());
                out.println();
                out.println(response);
                // remove enpoint for removing a room
            } else if (endpoint.equals("remove")) {
                String name = "";
                // Parse the request parameters
                for (String param : params) {
                    String[] pair = param.split("=");
                    if (pair[0].equals("name")) {
                        name = pair[1];
                        break;
                    }
                }
                // Remove the room from the database
                MongoCollection<Document> collection = database.getCollection("room");
                Document document = new Document("name", name);
                System.out.println("Document inserted successfully");
                int statusCode = 500;
                String response = "Received request to add room: " + name;
                // Check if the room already exists
                if (collection.find(document).first() != null) {
                    System.out.println("Document found successfully");
                    statusCode = 200;
                    collection.deleteOne(document);
                    response = "Room has been removed : " + name;
                } else { // If the room does not exist, add it to the database
                    System.out.println("Document not found");
                    statusCode = 403;
                    response = "Room does not exist";
                }
                // Send the response to the client
                out.println("HTTP/1.1 " + statusCode);
                out.println("Content-Type: text/html");
                out.println("Content-Length: " + response.length());
                out.println();
                out.println(response);
            } else if (endpoint.equals("reserve")) { // reserve enpoint for reserving a room
                String name = "";
                String day = "";
                String hour = "";
                String duration = "";
                // Parse the request parameters
                for (String param : params) {
                    String[] pair = param.split("=");
                    if (pair[0].equals("name")) {
                        name = pair[1];
                    } else if (pair[0].equals("day")) { // Check if the day is valid
                        day = (Integer.parseInt(pair[1]) > 0 && Integer.parseInt(pair[1]) < 8) ? pair[1] : "";
                    } else if (pair[0].equals("hour")) { // Check if the hour is valid
                        hour = (Integer.parseInt(pair[1]) > 8 && Integer.parseInt(pair[1]) < 18) ? pair[1] : "";
                    } else if (pair[0].equals("duration")) { // Check if the duration is valid
                        duration = (Integer.parseInt(pair[1]) + Integer.parseInt(hour) < 18) ? pair[1] : "";
                    }
                }
                // Reserve the room
                String response = "Received request to reserve room " + name + " on day " + day + " at hour " + hour
                        + " for duration " + duration + ".";
                System.out.println("Connected to the MongoClient successfully");
                MongoCollection<Document> collection = database.getCollection("room");
                Document document = new Document("name", name);
                int statusCode = 500;
                //Check if the input is valid
                if (name.equals("") || day.equals("") || hour.equals("") || duration.equals("")) {
                    statusCode = 400;
                    response = "Invalid input";
                } else { // Check if the room exists
                    if (collection.find(document).first() != null) {
                        collection = database.getCollection("reservation");

                        Document filter = new Document("name", name).append("day", day);

                        Document sort = new Document("hour", 1);
                        // Find the documents that match the filter
                        List<Document> results = collection.find(filter).sort(sort).into(new ArrayList<>());

                        for (Document doc : results) { // Check if the room is available
                            if (isBetween(doc.getString("hour"), hour, duration) || isBetweenWithDuration(
                                    doc.getString("hour"), doc.getString("duration"), hour, duration)) {
                                statusCode = 403;
                                response = "Room is not available"; // Burası biraz daha düzenlenecek
                                break;
                            }
                        }
                        if (statusCode != 403) { // If the room is available, reserve it
                            System.out.println("Document found successfully");
                            statusCode = 200;
                            ObjectId reservationId = new ObjectId();
                            Document document1 = new Document("name", name).append("day", day).append("hour", hour)
                                    .append("duration", duration).append("reservation_id", reservationId);
                            collection.insertOne(document1);
                            response = "Room has been reserved : " + name;
                        }
                    } else { // If the room does not exist
                        System.out.println("Document not found");
                        statusCode = 403;
                        response = "Room does not exist";
                    }
                }
                // Send the response to the client
                out.println("HTTP/1.1 " + statusCode);
                out.println("Content-Type: text/html");
                out.println("Content-Length: " + response.length());
                out.println();
                out.println(response);
            } else if (endpoint.equals("checkavailability")) { // checkavailability endpoint for checking the availability of a room
                String name = "";
                String day = "";
                // Parse the request parameters
                for (String param : params) {
                    String[] keyValue = param.split("=");
                    if (keyValue[0].equals("name")) {
                        name = keyValue[1];
                    } else if (keyValue[0].equals("day")) {
                        day = keyValue[1];
                    }
                }
                System.out.println("Connected to the MongoClient successfully");
                MongoCollection<Document> collection = database.getCollection("room");
                Document document = new Document("name", name);
                // Check if the room exists
                if (collection.find(document).first() == null) {
                    String response = "Room does not exist";
                    int statusCode = 404;
                    out.println("HTTP/1.1 " + statusCode);
                    out.println("Content-Type: text/html");
                    out.println("Content-Length: " + response.length());
                    out.println();
                    out.println(response);
                } else { // If the room exists, check the availability
                    collection = database.getCollection("reservation");

                    Document filter = new Document("name", name).append("day", day);
                    Document sort = new Document("hour", 1);
                    // Find the documents that match the filter
                    List<Document> results = collection.find(filter).sort(sort).into(new ArrayList<>());
                    HashSet<Integer> hours = new HashSet<Integer>();
                    // Add the hours to the hashset
                    for (Document doc : results) {
                        for (int i = Integer.parseInt(doc.getString("hour")); i < Integer
                                .parseInt(doc.getString("hour"))
                                + Integer.parseInt(doc.getString("duration")); i++) {
                            hours.add(i);
                        }
                    }
                    // Get the day from the day number
                    String response = getDay(day) + " ";
                    String availableDays = "";
                    // Check the availability for the hours
                    for (int i = 9; i < 18; i++) {
                        if (!hours.contains(i)) {
                            availableDays += i + " ";
                        }
                    }
                    int statusCode = 0;
                    if (!availableDays.equals("")) { // If the room is available, send the available hours
                        response += availableDays;
                        statusCode = 200;
                    } else { // If the room is not available, send the response
                        response = "Not available";
                        statusCode = 404;
                    }

                    // Send the response to the client
                    out.println("HTTP/1.1 " + statusCode);
                    out.println("Content-Type: text/html");
                    out.println("Content-Length: " + response.length());
                    out.println();
                    out.println(response);
                }
            }
            // Close the connection
            mongoClient.close();
            clientSocket.close();
        }
    }
}
