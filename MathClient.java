import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class MathClient {

    List<String> question;
    private static int currentQuestion;
    private static long startTime;
    private long totalChallengeTime; // in milliseconds
    BufferedReader br = null;
    PrintWriter pw = null;
    Socket soc = null;
    public static void main(String[] args) {
        start();


    }
    public static void start(){
        BufferedReader input = null;
        Socket soc = null;
        PrintWriter pw = null;
        BufferedReader br = null;
        try {
            soc = new Socket("localhost", 8888); // Establish connection to the server
            input = new BufferedReader(new InputStreamReader(System.in));
            pw = new PrintWriter(soc.getOutputStream(), true);
            br = new BufferedReader(new InputStreamReader(soc.getInputStream()));

            String bold = "\u001B[1mWelcome to the International Education Services Mathematics Competition\u001B[0m";

            System.out.println("\n\t \t"+ bold);
            System.out.println("\nAre you a Pupil or School Representative?");

            String who = input.readLine();//allow user input

            if (who.equalsIgnoreCase("pupil")) {

                pupil(soc ,input, pw, br);
            } else if (who.equalsIgnoreCase("school representative")) {
                soc = new Socket("localhost", 8788); // Connect to the representative port
                pw = new PrintWriter(soc.getOutputStream(), true);
                br = new BufferedReader(new InputStreamReader(soc.getInputStream()));
                schoolRepresentative(input, pw, br);
            } else {
                String er =  "\u001B[33m Invalid command!\u001B[0m";
                System.out.println(er);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (input != null) input.close();
                if (pw != null) pw.close();
                if (br != null) br.close();
                if (soc != null) soc.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static void pupil(Socket soc, BufferedReader input, PrintWriter pw, BufferedReader br) throws IOException {
        Scanner read = new Scanner(System.in);
        try {
            while (true) {
                System.out.println();
                System.out.println("a) New here? Enter 'register'");
                System.out.println("b) Already registered? Enter 'login'");
                System.out.println("c) Previous Menu");

                String str = read.nextLine();

                if (str.equalsIgnoreCase("register")) {
                    register(soc, input, pw, br);
                } else if (str.equalsIgnoreCase("login")) {
                    attemptClient(soc);
                    break;
                } else if (str.equalsIgnoreCase("previous menu")) {
                    start();
                    break; // Exit the loop and return to start method
                } else {
                    System.out.println("Invalid input... TRY AGAIN");
                }
            }
        } catch (NoSuchElementException e) {
            System.out.println("Error reading input: " + e.getMessage());
        } finally {
            read.close();
        }

    }// end of pupil

    public static void register(Socket soc ,BufferedReader input, PrintWriter pw, BufferedReader br) throws IOException {

        while (true) { // to keep the client looping
            System.out.println();
            //menu
            System.out.println("Enter the following commands to proceed:\n" +
                    " a) Register <username firstname lastname emailAddress " +
                    "date_of_birth school_registration_number image_file.png> \n" +
                    " b) viewChallenges \n c) Previous menu  \n d) Main Menu\"");

            String details = input.readLine();
            pw.println(details); // Send user input to the server


            if (details.equalsIgnoreCase("viewChallenges")) {
                String response = br.readLine();
                System.out.println(response); // Debug logging
                if (response.equalsIgnoreCase("please register first")) {
                    String er =  "\u001B[33m Please register before viewing challenges!\u001B[0m";
                    System.out.println(er+"\n");

                } else {
                    // Read the list of challenges from the server
                    List<String> challenges = new ArrayList<>();
                    String challenge;
                    while ((challenge = br.readLine()) != null && !challenge.equalsIgnoreCase("END")) {
                        challenges.add(challenge);
                    }

                    if (challenges.isEmpty()) {
                        System.out.println("No challenges available.");
                    } else {
//                        System.out.println("Challenges:");
                        for (String ch : challenges) {
                            System.out.println(ch);
                        }
                        // Asking user for username and challenge choices
                        System.out.print("Enter your username: ");
                        String username = input.readLine();
                        System.out.println();
                        System.out.println("Enter your chosen challenge numbers and names (e.g. 1 Algebra;2 Geometry):");
                        String choices = input.readLine();

                        // Send username and choices to the server
                        String userChoices = "username:" + username + " choices:" + choices;
                        pw.println(userChoices);
                    }
                    // Read and print the success message from the server
                    String successMessage = br.readLine();
                    System.out.println(successMessage);
                }
            } else if (details.startsWith("Register")) {
                String response = br.readLine();
                System.out.println("Response from server: " + response); // Debug logging
                if (response.equalsIgnoreCase("registration successful")) {
                    String bd =  "\u001B[33m You have been registered successfully!\u001B[0m";
                    System.out.println(bd +"\n");
                } else if (response.equalsIgnoreCase("already registered")) {
                    String bd =  "\u001B[33m You are already registered!\u001B[0m";
                    System.out.println(bd+"\n");
                } else if (response.equalsIgnoreCase("rejected")) {
                    String bd =  "\u001B[33m Your registration has been rejected!\u001B[0m";
                    System.out.println(bd+"\n");
                } else {
                    String bd = "\u001B[33m "+response+"\u001B[0m";
                    System.out.println("Registration failed: " + bd);
                }
            } else if (details.equalsIgnoreCase("previous menu")) {
                pw.println("previous menu");
                pupil(soc,input,pw,br);

            }else if(details.equalsIgnoreCase("main menu")){
                start();

            }else {
                System.out.println(br.readLine());
            }
        }
    } // end of register

    public static void schoolRepresentative(BufferedReader input, PrintWriter pw, BufferedReader br) throws IOException {
        System.out.println();
        String bold = "\u001B[1mWelcome, dear school representative, to our Mathematics Challenge System.\u001B[0m";
        System.out.println("\t"+bold);

        Scanner scan = new Scanner(System.in);
        boolean loggedIn =false;

        while (true) {
            System.out.println("Enter a command to Proceed ");
            if (!loggedIn) {
                System.out.println("Login");
            } else {
                System.out.println("Logout");
            }
            System.out.println("Previous menu");

            String option = scan.nextLine();
            if (option.equalsIgnoreCase("login") && !loggedIn) {
                System.out.print("Enter your name:");
                String name = input.readLine();

                System.out.print("Enter your password:");
                String password = input.readLine();

                System.out.println();
                String details = name +" "+password;
                pw.println(details); // Send details to server

                // Read and display server response
                String serverResponse = br.readLine();
                System.out.println("Server response: " + serverResponse); // Debug logging

                // If the server has verified the details, read the file content
                if ("Details verified successfully!".equals(serverResponse)) {
                    loggedIn=true;// Set the logged-in status to true
                    String fileLine;
                    while ((fileLine = br.readLine()) != null) {
                        if ("EOF".equals(fileLine)) { // Check for end-of-file marker
                            break;
                        }
                        System.out.println(fileLine);

                    }
                    System.out.println();
                    System.out.println("Enter the following commands in order to confirm the pupils");
                    System.out.println("Use the commands .........confirm yes/no username");
                    System.out.println("Leave a comma after every confirmation detail e.g., confirm yes username, confirm no username");

                    // Store the confirmations
                    String confirm = input.readLine();

                    // Send the confirmations to the server
                    pw.println(confirm);

                    // Read final confirmation response from the server
                    String confirmationResponse = br.readLine();
                    System.out.println(confirmationResponse);
                } else {
                    System.out.println("Verification failed: " + serverResponse);
                }
            } else if (option.equalsIgnoreCase("logout") && loggedIn) {
                loggedIn = false; // Set the logged-in status to false
                System.out.println("You have successfully logged out.");
                pw.println("logout");}
            else if(option.equalsIgnoreCase("previous menu")){
                pw.println("previous menu");
                start();
            }else {
                System.err.println("check your command");
            }
        }
    } // end of schoolRepresentative

    public MathClient(List<String> question, long totalChallengeTimeInMinutes, Socket soc) {
        this.question = question;
        this.currentQuestion = 0;
        this.totalChallengeTime = totalChallengeTimeInMinutes * 60 * 1000; // converting to milliseconds
        this.soc = soc;
    }

    public void showStatus( long questionStart,long questionEnd, List<String> question, List<String> questionReport,BufferedReader fromSoc) throws IOException {
        // BufferedReader fromSoc = null;
        // long questionEnd=System.currentTimeMillis();
        // fromSoc = new BufferedReader(new InputStreamReader(soc.getInputStream()));
        long duration = System.currentTimeMillis() - this.startTime;
        int remainingQuestions = this.question.size() - this.currentQuestion;


        long remainingTime = totalChallengeTime - duration;
        Long questiontime=questionEnd-questionStart;
        //System.out.println(question.size());
        //for(int i = 0; i < question.size(); i++) {
        int i = 0;
        questionReport.add( "Qn" +(i+1)+" "+"=>"+(questiontime/1000)+" seconds");
        //}
        if (remainingTime <= 0 && currentQuestion < question.size()) {
            System.out.println("Time is Up!");
            endSession(questionReport);
            System.out.println("                                    \nYour SCORE is: " + fromSoc.readLine());

        } else {



            System.out.println("Time used: "+((duration/1000) )+" seconds");
            System.out.println("Remaining Questions: " + remainingQuestions);
            System.out.println("Remaining time :" + (remainingTime / 1000) + " seconds");

        }
    }


    public void endSession(List<String> questionReport) {
        long totalTime = System.currentTimeMillis() - this.startTime;
        for(String br: questionReport) {
            System.out.println(br);
        }
        System.out.println("Total time spent: " + ((totalTime / 1000)) + " seconds");
    }

    public static void attemptClient(Socket soc) {
        BufferedReader br = null;
        BufferedReader fromSoc = null;
        PrintWriter pw = null;

        try {
            //System.out.println("Loading...");
            // Display some sort of impressive welcome message
            System.out.println("\n\t\tLETS ROLL!\nPlease enter your details to proceed");

            // read the details of the user i.e. the username and email and send
            // them to the server
            br = new BufferedReader(new InputStreamReader(System.in));
            String log = "login";
            System.out.print("Username: ");
            String name = br.readLine();
            System.out.print("Email: ");
            String email = br.readLine();
            String details = log+" "+ name + " " + email;

            pw = new PrintWriter(soc.getOutputStream(), true);
            pw.println(details);

            fromSoc = new BufferedReader(new InputStreamReader(soc.getInputStream()));
            List<String> questionReport = new ArrayList<>();

            if (fromSoc.readLine().equalsIgnoreCase("success")) {
                System.out.println("\nEnter the following commands to proceed:\n a) viewChallenges\n b)Exit\n");

                String input = br.readLine();

                if (input.equalsIgnoreCase("viewChallenges")) {
                    List<String> choice = new ArrayList<>();
//                    List<String> questionReport = new ArrayList<>();

                    String yourChoice = "";

                    while (!(yourChoice = fromSoc.readLine()).equalsIgnoreCase("END")) {
                        choice.add(yourChoice);
                    }

                    System.out.println("Your Choice:");

                    for (String j : choice) {
                        System.out.println(j);
                    }

                    System.out.println();
                    System.out.println("In order to Attempt challenge, Enter this command: attemptChallenge <challengeNumber> or exit to leave\n");
                    System.out.println("       ***INSTRUCTIONS\n  ->Enter your Answer on the next line just after the question\n  ->Enter - for notsure and it earns 0 marks\n  ->Correct answer earns you allocated marks\n  ->Wrong Answer earns you -3 marks\n  ->Choose wisely, let justice prevail\n");

                    String attemptChoice = br.readLine();
                    // exit method called
                    pw.println(attemptChoice);
                    // attemptClient(); there should be a method call here, butaaah hmmmm
                    System.out.println("You are always welcome to visit!");


                    pw.println(attemptChoice);

                    // receiving questions from the Server
                    List<String> question = new ArrayList<>();
                    // instructions
                    String qn = "";

                    while (!(qn = fromSoc.readLine()).equalsIgnoreCase("done")) {
                        question.add(qn);
                    }

                    System.out.println();
                    System.out.print("COUNT DOWN STARTS NOW!");

                    MathClient mc = new MathClient(question, 1, soc);

                    br = new BufferedReader(new InputStreamReader(System.in));

                    pw = new PrintWriter(soc.getOutputStream(), true);
                    startTime = System.currentTimeMillis();
                    for (String quiz : question) {
                        System.out.println("\n" + quiz);
                        long questionStart=System.currentTimeMillis();
                        // receiving the answer per question
                        String ans = br.readLine();
                        long questionEnd=System.currentTimeMillis();


                        pw.println(ans);
                        currentQuestion++;

                        mc.showStatus(questionStart,questionEnd, question, questionReport,fromSoc); // showing update
                    }
                    System.out.println("\n Report");

                    mc.endSession(questionReport); // at finishing
                    // printing out your score in that particular challenge
                    System.out.println("\n                      Your SCORE is: " + fromSoc.readLine());
                    // System.out.println("You can only attempt the challenge three times, enter yes if you want to attempt again");
                    // String another = br.readLine();
                    // pw.println(another);

                }
            } else {
                String bd =  "\u001B[33m Check your credentials and try again or register!\u001B[0m";
                System.out.println(bd);
            }
            // my loop bracket
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (br != null)
                    br.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            try {
                if (fromSoc != null)
                    fromSoc.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            if (pw != null)
                pw.close();
            try {
                if (soc != null)
                    soc.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }
}