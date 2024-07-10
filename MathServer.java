import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MathServer {
    public static void main(String[] args) {
        register();

    }//main
    public static void register() {
        //declarations
        ServerSocket ss = null;
        Socket soc = null;
        BufferedReader br = null;
        PrintWriter pw = null;
        Connection con = null;
        BufferedReader out = null;
        boolean register = false;


        System.out.println("waiting ...");

        // Establish database connection
        try {
            con = DriverManager.getConnection("jdbc:mysql://localhost:3306/mathchallenge", "root", "");
        } catch (SQLException sqle) {
            System.err.println("SQLException: " + sqle.getMessage());
            return; // Exit if database connection fails
        }

        try {
            // Create server socket to accept client connection
            ss = new ServerSocket(8888);
            soc = ss.accept();
            pw = new PrintWriter(soc.getOutputStream(), true);

            // Read input from client
            br = new BufferedReader(new InputStreamReader(soc.getInputStream()));
            String details = br.readLine();

            if (details == null) {//new--research
                System.out.println("No data received from client.");
                return;
            }
            System.out.println(details);

            String[] detail = details.split(" ");//splitting the received data into an array of strings
            if (detail.length == 0) {//new checkout
                pw.println("Invalid command format.");
                return;
            }

            if (detail[0].equalsIgnoreCase("register")) { // if register option is selected
                String regno = detail[6];
                String email = detail[4];
                // Use PreparedStatement to prevent SQL injection and improve performance
                String query = "SELECT regNo FROM school WHERE regNo = ?";
                try (PreparedStatement pstmt = con.prepareStatement(query)) {
                    pstmt.setString(1, regno);
                    ResultSet rs = pstmt.executeQuery();

                    if (rs.next()) {//checks for any rows and moves to the next row
                        String result = rs.getString("regNo");//retrives column value from current dataset

                        if (regno.equalsIgnoreCase(result)) {
                            //check against the rejected table
                            String rejectQuery = "SELECT regNo, email FROM rejected WHERE regNo = ? AND email = ?";
                            try (PreparedStatement rejectPstmt = con.prepareStatement(rejectQuery)) {
                                rejectPstmt.setString(1, regno);
                                rejectPstmt.setString(2, email);
                                ResultSet rejectRs = rejectPstmt.executeQuery();
                                //if found
                                if (rejectRs.next()) {
                                    //the client is informed
                                    pw.println("you cannot register under this school");
                                }
                                //if not found
                                else {
                                    //add to file
                                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File("file.csv"), true))) {
                                        writer.write("\n" + detail[1] + " " + detail[2] + " " + detail[3] + " " + detail[4] + " " + detail[6]
                                                + " " + detail[7]);
                                    } catch (IOException ioe) {
                                        System.err.println("IOException while writing to file: " + ioe.getMessage());
                                    }
                                    // Send success message to client
                                    pw.println("successfully registered!");
                                    register=true;

                                }

                            } catch (SQLException sqle) {
                                System.err.println("SQLException: " + sqle.getMessage());
                            }
                        }
                    } else {
                        pw.println("Registration number not found");
                    }

                } catch (SQLException sqle) {
                    System.err.println("SQLException: " + sqle.getMessage());
                }
            }//end of first if

            else if (detail[0].equalsIgnoreCase("viewChallenges")) {
                if(register) {
                    //statement to execute query
                    Statement stmt = con.createStatement();
                    String s = "SELECT challengeID, challengeName FROM Challenge";
                    ResultSet rs = stmt.executeQuery(s);
                    //a list to store the challenges
                    List<String> challenges = new ArrayList<>();

                    // Iterate through the result set
                    while (rs.next()) {
                        // Retrieve the challenge ID and name from the current row
                        String id = rs.getString("challengeID");
                        String name = rs.getString("challengeName");
                        // Add the challenge ID and name to the list
                        challenges.add(id + "  " + name);
                    }
                    pw = new PrintWriter(soc.getOutputStream(), true);

                    // Send all records to the client
                    for (String challenge : challenges) {
                        pw.println(challenge);
                    }
                    // Send end-of-stream signal
                    pw.println("END");

                    //CODE TO RECEIVE CHOICES
                    out = new BufferedReader(new InputStreamReader(soc.getInputStream()));
                    String choices = br.readLine();//checkout
                    String[] myChoice = choices.split(" ");
                    for (String C : myChoice) {
                        System.out.println(C);
                    }
                }else{
                    pw.println("please register first");
                    System.out.println("first register");
                }
            }
            else {
                pw.println("Check your command and TRY AGAIN ....!!!");
            }
        } catch (IOException ioe) {
            System.err.println("IOException: " + ioe.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //close resources
            try {
                if (br != null) br.close();
                if (pw != null) pw.close();
                if (soc != null) soc.close();
                if (ss != null) ss.close();
                if (con != null) con.close();
            } catch (IOException | SQLException e) {
                System.err.println("Exception during cleanup: " + e.getMessage());
            }
        }//finally

    }//method register
}// class