// Author: Mustafa Yanar, Emir Said Haliloğlu, Yiğit Göksel
// Date: 12/12/2020
// Computer Network Project : Reservation Server

import java.io.*;
import java.net.*;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class ReservationServer {

    public static void main(String[] args) throws Exception {
        // Set the port number
        int port = 8082;

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

            if (endpoint.equals("reserve")) { // Reserve a room
                String name = "";
                String activity = "";
                String day = "";
                String hour = "";
                String duration = "";
                // Parse the request parameters
                for (String param : params) {
                    String[] pair = param.split("=");
                    if (pair[0].equals("room")) {
                        name = pair[1];
                    } else if (pair[0].equals("activity")) {
                        activity = pair[1];
                    } else if (pair[0].equals("day")) {
                        day = pair[1];
                    } else if (pair[0].equals("hour")) {
                        hour = pair[1];
                    } else if (pair[0].equals("duration")) {
                        duration = pair[1];
                        break;
                    }
                }
                int statusCode = 500;
                String response = "Received request to: " + activity;
                // Check if the activity exists
                Socket activitySocket = new Socket("localhost", 8081);
                BufferedReader activityIn = new BufferedReader(new InputStreamReader(activitySocket.getInputStream()));
                PrintWriter activityOut = new PrintWriter(activitySocket.getOutputStream(), true);
                // Send the request to the activity server
                activityOut.println("GET /check?name=" + activity + " HTTP/1.1");
                // activityResponses[0] = "HTTP/1.1" activityResponses[1] = "200" etc.
                String[] activityResponse = activityIn.readLine().split(" ");
 
                if (activityResponse.length == 2) { // If the response is valid
                    if (activityResponse[1].equals("200")) { // If the activity exists
                        // Check if the room exists
                        Socket roomSocket = new Socket("localhost", 8080);
                        BufferedReader roomIn = new BufferedReader(new InputStreamReader(roomSocket.getInputStream()));
                        PrintWriter roomOut = new PrintWriter(roomSocket.getOutputStream(), true);
                        // Send the request to the room server
                        roomOut.println("GET /reserve?name=" + name + "&day=" + day + "&hour=" + hour + "&duration="
                                + duration + " HTTP/1.1");
                        String[] roomResponse = roomIn.readLine().split(" ");

                        if (roomResponse.length == 2) { // If the response is valid
                            if (roomResponse[1].equals("200")) { // If the room exists
                                statusCode = 200;
                                response = "Reservation successful";
                            } else if (roomResponse[1].equals("403")) { // If the room is already reserved
                                System.out.println("Room is already reserved");
                                statusCode = 403;
                                response = "Room does not exist";
                            } else if (roomResponse[1].equals("400")) { // If the room does not exist
                                System.out.println("Invalid Input");
                                statusCode = 400;
                                response = "Invalid Input";
                            }
                        } else { // If the response is invalid
                            System.out.println("Something went wrong with the room server");
                            statusCode = 500;
                            response = "Server Error Line:139";
                        }
                        roomSocket.close();
                    } else if (activityResponse[1].equals("404")) { // If the activity does not exist
                        statusCode = 404;
                        response = "Activity does not exist";
                    }
                } else { // If the response is invalid
                    System.out.println("Something went wrong with the activity server");
                    statusCode = 500;
                    response = "Server Error Line:149";
                }
                activitySocket.close();

                // Send the response to the client
                out.println("HTTP/1.1 " + statusCode);
                out.println("Content-Type: text/html");
                out.println("Content-Length: " + response.length());
                out.println();
                out.println(response);
            } else if (endpoint.equals("listavailability")) { // List the availability of a room
                String room = "";
                String day = "";
                // Parse the request parameters
                for (String param : params) {
                    String[] pair = param.split("=");
                    if (pair[0].equals("room")) {
                        room = pair[1];
                    } else if (pair[0].equals("day")) {
                        day = pair[1];
                        break;
                    }
                }
                int statusCode = 500;
                String response = "";
                // Check if the room is available for the given day
                if (!day.equals("") && Integer.parseInt(day) > 0 && Integer.parseInt(day) < 8) {
                    Socket roomSocket = new Socket("localhost", 8080);
                    BufferedReader roomIn = new BufferedReader(new InputStreamReader(roomSocket.getInputStream()));
                    PrintWriter roomOut = new PrintWriter(roomSocket.getOutputStream(), true);
                    // Send the request to the room server
                    roomOut.println("GET /checkavailability?name=" + room + "&day=" + day + " HTTP/1.1");
                    String[] roomResponse = roomIn.readLine().split(" ");

                    System.out.println("Received request to list availability for: " + room);
                    if (roomResponse.length == 2) { // If the response is valid
                        if (roomResponse[1].equals("200")) {
                            statusCode = 200;
                            roomIn.readLine();
                            roomIn.readLine();
                            roomIn.readLine();
                            // Get the availability of the room
                            response = roomIn.readLine();
                        } else if (roomResponse[1].equals("404")) { // If the room does not exist
                            System.out.println("No Such Room Exists");
                            statusCode = 404;
                            response = "No Such Room Exists";
                        } else if (roomResponse[1].equals("400")) { // If the room does not exist
                            System.out.println("Invalid Input");
                            statusCode = 400;
                            response = "Invalid Input";
                        }
                    } else { // If the response is invalid
                        System.out.println("Something went wrong with the room server");
                        statusCode = 500;
                        response = "Server Error Line:201";
                    }
                    roomSocket.close();
                } else { // If the day is invalid
                    for (int i = 1; i < 8; i++) { // Check the availability of the room for each day
                        day = Integer.toString(i);
                        Socket roomSocket = new Socket("localhost", 8080);
                        BufferedReader roomIn = new BufferedReader(new InputStreamReader(roomSocket.getInputStream()));
                        PrintWriter roomOut = new PrintWriter(roomSocket.getOutputStream(), true);
                        // Send the request to the room server
                        roomOut.println("GET /checkavailability?name=" + room + "&day=" + day + " HTTP/1.1");
                        String[] roomResponse = roomIn.readLine().split(" ");

                        System.out.println("Received request to list availability for: " + room);

                        if (roomResponse.length == 2) { // If the response is valid
                            if (roomResponse[1].equals("200")) {
                                statusCode = 200;
                                roomIn.readLine();
                                roomIn.readLine();
                                roomIn.readLine();
                                response += roomIn.readLine() + "<br>"; // Get the availability of the room
                            } else if (roomResponse[1].equals("404")) { // If the room does not exist
                                System.out.println("No Such Room Exists");
                                statusCode = 404;
                                response = "No Such Room Exists";
                            } else if (roomResponse[1].equals("400")) { // If the room does not exist
                                System.out.println("Invalid Input");
                                statusCode = 400;
                                response = "Invalid Input";
                            }
                        } else { // If the response is invalid
                            System.out.println("Something went wrong with the room server");
                            statusCode = 500;
                            response = "Server Error Line:234";
                        }
                        roomSocket.close();
                    }
                }
                // Send the response to the client
                out.println("HTTP/1.1 " + statusCode);
                out.println("Content-Type: text/html");
                out.println("Content-Length: " + response.length());
                out.println();
                out.println(response);
            } else if (endpoint.equals("display")) {
                String id = "";
                // Parse the request parameters
                for (String param : params) {
                    String[] pair = param.split("=");
                    if (pair[0].equals("id")) {
                        id = pair[1];
                    }
                }
                int statusCode = 0;
                String response = "";
                System.out.println("Connected to the MongoClient successfully");
                MongoCollection<Document> collection = database.getCollection("reservation");
                ObjectId objectId = new ObjectId(id);
                Document document = new Document("reservation_id", objectId);
                if (!id.equals("")) { // If the id is valid
                    if (collection.find(document).first() != null) { // If the reservation exists
                        System.out.println("Document found successfully");
                        statusCode = 200;
                        response = collection.find(document).first().toJson();
                    } else { // If the reservation does not exist
                        System.out.println("Document not found");
                        statusCode = 404;
                        response = "Reservation does not exist";
                    }
                } else {
                    statusCode = 500;
                    response = "Error";
                }
                // Send the response to the client
                out.println("HTTP/1.1 " + statusCode);
                out.println("Content-Type: text/html");
                out.println("Content-Length: " + response.length());
                out.println();
                out.println(response);
            }
            // Close the connection
            mongoClient.close();
            clientSocket.close();
        }
    }
}
