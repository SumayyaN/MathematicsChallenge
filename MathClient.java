import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class MathClient {
    public static void main(String[] args) throws IOException {
        BufferedReader input = null;
        Socket soc = null;
        try {
            System.out.println("Welcome to the International Education Services Mathematics Competition ");
            System.out.println("Are you a Pupil or School Representative");

            // Read user input from the console
            input = new BufferedReader(new InputStreamReader(System.in));
            String who = input.readLine();

            if (who.equalsIgnoreCase("pupil")) {
                pupil();
            } else if (who.equalsIgnoreCase("school representative")) {
                System.out.println("Code will go here");
            } else {
                System.out.println("Invalid command");
            }
        } finally {
            if (input != null) input.close();
            if (soc != null) soc.close();
        }
    }//end of main

    public static void pupil() {
        System.out.println("a) New here? Enter 'register'");
        System.out.println("b) Already registered? Enter 'login'");
        Scanner read = new Scanner(System.in);
        String str = read.nextLine();
        try {
            if (str.equalsIgnoreCase("register")) {
                register();
            } else if (str.equalsIgnoreCase("login")) {
                System.out.println("Code for login");
            } else {
                System.out.println("Invalid input... TRY AGAIN");
            }
        } finally {
            read.close();
        }
    }//end of pupil

    public static void register() {
        Socket soc = null;
        BufferedReader input = null;
        BufferedReader bf = null;

        try {
            System.out.println("Loading...");
            // Establish connection to the server
            soc = new Socket("localhost", 8888);

            while (true) { // to keep the client looping
                System.out.println("Enter the following commands to proceed:\n" +
                        " a) Register <username firstname lastname emailAddress " +
                        "date_of_birth school_registration_number image_file.png> \n" +
                        " b) viewChallenges \n c) Exit");

                // Read user input from the console
                input = new BufferedReader(new InputStreamReader(System.in));
                String details = input.readLine();

                // Send user input to the server
                PrintWriter pw = new PrintWriter(soc.getOutputStream(), true);
                pw.println(details);

                // Notifications for registration or viewing challenges
                bf = new BufferedReader(new InputStreamReader(soc.getInputStream()));

                if (details.equalsIgnoreCase("viewChallenges")) {
                    String response = bf.readLine();
                    if (response.equalsIgnoreCase("please register first")) {
                        System.out.println("Please register before viewing challenges.");}
                    else {
                        // Read the list of challenges from the server
                        List<String> challenges = new ArrayList<>();
                        String challenge;
                        while ((challenge = bf.readLine()) != null && !challenge.equalsIgnoreCase("END")) {
                            challenges.add(challenge);
                        }

                        if (challenges.isEmpty()) {
                            System.out.println("No challenges available.");
                        } else {
                            System.out.println("Challenges:");
                            for (String ch : challenges) {
                                System.out.println(ch);
                            }

                            System.out.println("Enter your choices (e.g., 1 2 3):");
                            String choices = input.readLine();
                            pw.println(choices);
                        }
                    }
                } else {
                    // For registration or other commands
                    System.out.println(bf.readLine());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (input != null) input.close();
                if (bf != null) bf.close();
                if (soc != null) soc.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }//end of register
}