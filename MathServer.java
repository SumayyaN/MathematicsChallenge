import java.io.*;
import java.net.*;
import java.net.Authenticator;
import java.nio.file.Files;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.lang.Math;
import java.util.Properties;
import javax.mail.PasswordAuthentication;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;


public class MathServer {
    private static final int PUPIL_PORT = 8888;
    private static final int REPRESENTATIVE_PORT = 8788;

    public static void main(String[] args) {
        try {
            ServerSocket pupilServerSocket = new ServerSocket(PUPIL_PORT);
            ServerSocket representativeServerSocket = new ServerSocket(REPRESENTATIVE_PORT);

            while (true) {
                new Thread(new ClientHandler(pupilServerSocket.accept(), true)).start();
                new Thread(new ClientHandler(representativeServerSocket.accept(), false)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private boolean isPupil;

        public ClientHandler(Socket socket, boolean isPupil) {
            this.socket = socket;
            this.isPupil = isPupil;
        }

        @Override
        public void run() {
            try (
                    BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
                    Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/mathchallenge", "root", "")
            ) {
                if (isPupil) {
                    handlePupil(br, pw, con);
                    System.out.println("Handling pupil connection");
                } else {
                    handleRepresentative(br, pw, con);
                    System.out.println("Handling school representative connection");
                }
            } catch (IOException | SQLException e) {
                e.printStackTrace();
            }
        }

        private void handlePupil(BufferedReader br, PrintWriter pw, Connection con) throws IOException, SQLException {
            boolean register = false;
            while (true) {
                String details = br.readLine();
                if (details == null || details.equalsIgnoreCase("previous menu")) {
                    pw.println("Goodbye!");
                    run();
                    break;

                }

                System.out.println("Received: " + details); // Logging

                String[] detail = details.split(" ");
                if (detail.length == 0) {
                    pw.println("Invalid command format.");
                    continue;
                }

                if (detail[0].equalsIgnoreCase("register")) {
                    if (detail.length < 8) {
                        pw.println("Invalid registration command format.");
                        continue;
                    }

                    String regno = detail[6];
                    String email = detail[4];
                    String imageFile = detail[7];
                    System.out.println(regno + email + imageFile);//debug
                    //compare reg number with that in the school table
                    String query = "SELECT regNo FROM schools WHERE regNo = ?";

                    try (PreparedStatement pstmt = con.prepareStatement(query)) {
                        pstmt.setString(1, regno);
                        ResultSet rs = pstmt.executeQuery();

                        if (rs.next()) {
                            String result = rs.getString("regNo");
                            if (regno.equalsIgnoreCase(result)) {
                                //check the rejected table
                                String rejectQuery = "SELECT regNo, email FROM rejected WHERE regNo = ? AND email = ?";
                                try (PreparedStatement rejectPstmt = con.prepareStatement(rejectQuery)) {
                                    rejectPstmt.setString(1, regno);
                                    rejectPstmt.setString(2, email);
                                    System.out.println(regno + email);//debug
                                    ResultSet rejectRs = rejectPstmt.executeQuery();
                                    if (rejectRs.next()) {//if record is in rejected
                                        pw.println("You cannot register under this school");
                                    } else {
                                        // Read image file into byte array
                                        byte[] imageBytes = null;
                                        try {
                                            File file = new File(imageFile); //should contain the path to the image file
                                            imageBytes = Files.readAllBytes(file.toPath());
                                            System.out.println(imageBytes);
                                        } catch (IOException e) {
                                            System.err.println("Error reading image file: " + e.getMessage());
                                            pw.println("Error reading image file.");
                                            continue;
                                        }

                                        // Save the image to a directory
                                        File destFile = new File("images/" + new File(imageFile).getName());
                                        try (FileOutputStream fos = new FileOutputStream(destFile)) {
                                            fos.write(imageBytes);
                                            System.out.println("Image saved to filesystem successfully.");
                                        } catch (IOException e) {
                                            System.err.println("Error saving image to filesystem: " + e.getMessage());
                                            pw.println("Error saving image to filesystem.");
                                            continue;
                                        }

                                        // Write user data to a CSV file
                                        try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File("file.csv"), true))) {
                                            writer.write("\n" + detail[1] + " " + detail[2] + " " + detail[3] + " " + detail[4] + " " + detail[5] + " " + detail[6] + " " + detail[7]);
                                        } catch (IOException ioe) {
                                            System.err.println("IOException while writing to file: " + ioe.getMessage());
                                            pw.println("Error writing user data to file.");
                                            continue;
                                        }

                                        pw.println("Successfully registered!");
                                        register = true;

                                        // Send email after registration
                                        sendConfirmationEmail(regno, con);
                                    }
                                }
                            } else {
                                pw.println("Registration number not found.");
                            }
                        } else {
                            pw.println("Registration number not found.");
                        }
                    }
                } else if (details.equalsIgnoreCase("viewChallenges")) {
                    if (register) {
                        Statement stmt = con.createStatement();
                        String s = "SELECT challengeNo, challengeName FROM challenges";
                        ResultSet rs = stmt.executeQuery(s);
                        List<String> challenges = new ArrayList<>();

                        while (rs.next()) {
                            String id = rs.getString("challengeNo");
                            String name = rs.getString("challengeName");
                            challenges.add(id + "  " + name);
                        }
                        for (String challenge : challenges) {
                            pw.println(challenge);
                        }
                        pw.println("END");

                        String userChoices = br.readLine();
                        String[] parts = userChoices.split(" choices:");
                        String username = parts[0].split("username:")[1].trim();
                        String[] choicesArray = parts[1].split(";");

                        for (String choice : choicesArray) {
                            String[] choiceParts = choice.trim().split(" ", 2);
                            if (choiceParts.length == 2) {
                                String challengeNumber = choiceParts[0];
                                String challengeName = choiceParts[1];

                                String insertQuery = "INSERT INTO challengechoices (userName, challengeNo, challengeName) VALUES (?, ?, ?)";
                                try (PreparedStatement insertPstmt = con.prepareStatement(insertQuery)) {
                                    insertPstmt.setString(1, username);
                                    insertPstmt.setString(2, challengeNumber);
                                    insertPstmt.setString(3, challengeName);
                                    insertPstmt.executeUpdate();
                                }
                            }
                        }
                        pw.println("Challenges successfully chosen!");
                    } else {
                        pw.println("Please register first");
                    }
                } else if (detail[0].equalsIgnoreCase("login")) {
                    handleClient(br, con, pw, detail);
                    break;


                } else {
                    pw.println("Check your command and try again!");
                }
            }
        }

        private void handleRepresentative(BufferedReader bf, PrintWriter pw, Connection con) throws IOException, SQLException {
            while (true) {
                String details = bf.readLine();
                if (details == null || details.equalsIgnoreCase("Previous menu")) {
                    run();
                    return;
                }
                System.out.println("Received: " + details); // Logging
                String[] mydetails = details.split(" ");
                String name = mydetails[0] +" "+ mydetails[1];
//                String email = mydetails[2];
//                String regNo = mydetails[3];
                String password =mydetails[2];

                String query = "SELECT regNo FROM schools WHERE school_representative_name = ? AND representativePassword = ?";
                try (PreparedStatement pst = con.prepareStatement(query)) {
                    pst.setString(1, name);
//                    pst.setString(2, email);
                    pst.setString(2, password);
                    ResultSet rs = pst.executeQuery();
                    if (rs.next()) {
                        String regNo = rs.getString("regNo");
                        pw.println("Details verified successfully!");

                        sendFileContent(pw, regNo);
                        pw.println("EOF");

                        String getConfirmation = bf.readLine();
                        String[] confirmations = getConfirmation.split(",");
                        for (String confirmation : confirmations) {
                            processConfirmation(con, confirmation.trim(), regNo);
                        }
                        pw.println("Confirmations Successful.");
                    } else {
                        pw.println("Check your details");
                    }
                }
            }
        }

        private void sendFileContent(PrintWriter pw, String regNo) {
            String fileName = "file.csv";
            try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.contains(regNo)) {
                        pw.println(line);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void processConfirmation(Connection con, String confirmation, String regNo) throws SQLException {
            String[] parts = confirmation.split(" ");
            if (parts.length < 3) {
                System.err.println("Invalid confirmation format: " + confirmation);
                return;
            }

            String command = parts[1];
            String username = parts[2];
            String fileName = "file.csv";
            List<String> fileLines = new ArrayList<>();

            try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.contains(username) && line.contains(regNo)) {
                        String[] studentDetails = line.split(" ");
                        if (studentDetails.length < 7) {
                            System.err.println("Invalid student details format: " + line);
                            continue;
                        }

                        String studentUsername = studentDetails[0];
                        String firstName = studentDetails[1];
                        String lastName = studentDetails[2];
                        String email = studentDetails[3];
                        String dateOfBirth = studentDetails[4];
                        String schoolRegistrationNumber = studentDetails[5];
                        String imageFile = studentDetails[6];

                        // Read image file into byte array
                        byte[] imageBytes = null;
                        try {
                            File file = new File(imageFile);
                            imageBytes = Files.readAllBytes(file.toPath());
                        } catch (IOException e) {
                            System.err.println("Error reading image file: " + e.getMessage());
                            return;
                        }

                        String targetTable = command.equals("yes") ? "accepted" : "rejected";
                        String insertQuery = "INSERT INTO " + targetTable + " (userName, firstName, lastName, email, dateOfBirth, regNo, imageFile ,image) VALUES (?, ?, ?, ?, ?, ?, ? ,?)";
                        try (PreparedStatement pst = con.prepareStatement(insertQuery)) {
                            pst.setString(1, studentUsername);
                            pst.setString(2, firstName);
                            pst.setString(3, lastName);
                            pst.setString(4, email);
                            pst.setString(5, dateOfBirth);
                            pst.setString(6, schoolRegistrationNumber);
                            pst.setString(7, imageFile);
                            pst.setBytes(8, imageBytes);
                            pst.executeUpdate();
                        }
                        sendResultEmail(studentUsername,email, command);

                        continue; // Skip adding the current line to the fileLines

                    } else {
                        fileLines.add(line);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName))) {
                for (String fileLine : fileLines) {
                    bw.write(fileLine);
                    bw.newLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void handleClient(BufferedReader br, Connection con, PrintWriter pw, String[] credential) throws IOException, SQLException {
            String username = credential[1];
            String email = credential[2];

            String query = "SELECT userName FROM accepted WHERE userName = ?";
            try (PreparedStatement state = con.prepareStatement(query)) {
                state.setString(1, username);
                ResultSet rs = state.executeQuery();

                if (rs.next()) {
                    String result = rs.getString("userName");

                    if (username.equalsIgnoreCase(result)) {
                        String que = "SELECT email FROM accepted WHERE userName = ?";
                        try (PreparedStatement stmt = con.prepareStatement(que)) {
                            stmt.setString(1, username);
                            ResultSet eme = stmt.executeQuery();

                            if (eme.next()) {
                                String effect = eme.getString("email");

                                if (email.equalsIgnoreCase(effect)) {
                                    retrieveChallenges(username, con, pw);
                                } else {
                                    pw.println("Check and correct your details");
                                }
                            }
                        }
                    }
                } else {
                    pw.println("Check your credentials or register with us please!");
                }
                pw.println("END");
            }

            // Handle challenge attempt
            handleChallengeAttempt(username, email, socket, con, pw);
        }
        private void sendResultEmail(String user, String email, String status) {
            String subject = "Registration Status";
            String success="Congragulations dear " + user+" We are thrilled to inform you that you have been accepted "+
                    " to participate in the International Mathematics Competition ";
            String fail ="Dear "+user+ " we regret to inform you that you have been rejected and can not participate" +
                    " in the International Mathematics Competition ";

            String body = status.equalsIgnoreCase("yes") ? success : fail;
            sendEmail(email, subject, body);
        }

        private void retrieveChallenges(String username, Connection con, PrintWriter pw) throws SQLException {
            String challChoice = "SELECT challengeName, challengeNo FROM challengechoices WHERE userName = ?";
            try (PreparedStatement ps = con.prepareStatement(challChoice)) {
                ps.setString(1, username);
                ResultSet choice = ps.executeQuery();
                List<String> yourChallenge = new ArrayList<>();

                while (choice.next()) {
                    String chaName = choice.getString("challengeName");
                    String chaId = choice.getString("challengeNo");
                    yourChallenge.add(chaName + "\t" + chaId);
                }

                pw.println("Success");
                for (String x : yourChallenge) {
                    pw.println(x);
                }
            }
        }

        private void handleChallengeAttempt(String username, String email, Socket soc, Connection con, PrintWriter pw) throws IOException, SQLException {
            BufferedReader reader = new BufferedReader(new InputStreamReader(soc.getInputStream()));
            String[] atemChoice = reader.readLine().split(" ");

            if (atemChoice[0].equalsIgnoreCase("attemptChallenge")) {
                String challengeNumber = atemChoice[1];
                String chalquery = "SELECT challengeNo FROM challenges WHERE challengeNo=?";
                try (PreparedStatement psmt = con.prepareStatement(chalquery)) {
                    psmt.setString(1, challengeNumber);
                    ResultSet cs = psmt.executeQuery();
                    if (cs.next()) {
                        // Retrieve challenge questions
                        String challengeTable = "SELECT * FROM question JOIN answer on answer.answerNo = question.questionNo WHERE challengeNo = ?";
                        System.out.println(challengeTable);
                        try (PreparedStatement que = con.prepareStatement(challengeTable)) {
                            que.setString(1, challengeNumber);
                            ResultSet chalDetails = que.executeQuery();
                            List<String> questions = new ArrayList<>();

                            while (chalDetails.next()) {
                                int number =chalDetails.getInt("questionNo");
                                String question = chalDetails.getString("question");
                                int marks = chalDetails.getInt("mark");
                                questions.add(question + " (mark " + marks + ")");
                            }

                            List<String> randomQuestions = randomPick(questions, 1);
                            for (String myquestion : randomQuestions) {
                                pw.println(myquestion);
                            }
                            pw.println("DONE");

                            // Read answers and calculate score
                            calculateScore(challengeNumber, username, email, soc, con, pw);
                        }
                    }
                }
//            } else if (atemChoice[0].equalsIgnoreCase("exit")) {
//                pw.println("....");
            }
        }

        private void calculateScore(String challengeNumber, String username, String email, Socket soc, Connection con, PrintWriter pw) throws IOException, SQLException {
            BufferedReader answers = new BufferedReader(new InputStreamReader(soc.getInputStream()));
            int score = 0;
            int attemptNo=1;
            for(int i = 0; i < 2; i++ ) {
                String ans = answers.readLine();
                // System.out.println(ans);
                String ansQuery = "SELECT answer FROM question join answer on answer.answerNo = question.questionNo WHERE challengeNo = ?";
                try (PreparedStatement ansStmt = con.prepareStatement(ansQuery)) {
                    ansStmt.setString(1, challengeNumber);
                    ResultSet ansResult = ansStmt.executeQuery();

//                while(ansResult.next()) {
//                	String tableAns = ansResult.getString("answer");
//
//                	System.out.println(tableAns);
//                }

                    String tableAns = "";
                    String result="";
                    if (ansResult.next()) {
                        tableAns = ansResult.getString("answer");
                        System.out.println(tableAns);
                        if (ans.equalsIgnoreCase(tableAns)) {
                            //System.out.println(tableAns);
                            String markQuery = "SELECT mark FROM question join answer on answer.answerNo = question.questionNo WHERE answer = ?";
                            try (PreparedStatement markStmt = con.prepareStatement(markQuery)) {
                                markStmt.setString(1, tableAns);
                                ResultSet markset = markStmt.executeQuery();
                                while (markset.next()) {
                                    int x = markset.getInt("mark");
                                    // System.out.println(x);
                                    score += x;
                                    result="correct";
                                    String q="Select question,questionNo FROM question join answer on answer.answerNo = question.questionNo WHERE answer ="+tableAns;
                                    try(PreparedStatement st= con.prepareStatement(q)){
                                        ResultSet rs= st.executeQuery();
                                        while(rs.next()){
                                            int num=rs.getInt("questionNo");
                                            String qn= rs.getString("question");

                                            String insert="insert into challengeattempt(attemptNo,challengeNo,userName,questionNo,comment) VALUES(?,?,?,?,?)";
                                            try(PreparedStatement ps= con.prepareStatement(insert)){
                                                ps.setInt(1,attemptNo);
                                                ps.setString(2,challengeNumber);
                                                ps.setString(3,username);
                                                ps.setInt(4,num);
                                                ps.setString(5,result);
                                                ps.executeUpdate();

                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }else if (!(ans.equalsIgnoreCase(tableAns)) && !(ans.equals("-"))) {
                            score -= 3;
                            result="wrong";
                            String q="Select question,questionNo FROM question join answer on answer.answerNo = question.questionNo WHERE answer ="+tableAns;
                            try(PreparedStatement st= con.prepareStatement(q)){
                                ResultSet rs= st.executeQuery();
                                while(rs.next()){
                                    int num=rs.getInt("questionNo");
                                    String qn= rs.getString("question");

                                    String insert="insert into challengeattempt(attemptNo,challengeNo,userName,questionNo,comment) VALUES(?,?,?,?,?)";
                                    try(PreparedStatement ps= con.prepareStatement(insert)){
                                        ps.setInt(1,attemptNo);
                                        ps.setString(2,challengeNumber);
                                        ps.setString(3,username);
                                        ps.setInt(4,num);
                                        ps.setString(5,result);
                                        ps.executeUpdate();

                                    }
                                }
                            }

                        } else if (ans.equals("-")) {
                            score = score;
                            result="wrong";
                            String q="Select question,questionNo FROM question join answer on answer.answerNo = question.questionNo WHERE answer ="+tableAns;
                            try(PreparedStatement st= con.prepareStatement(q)){
                                ResultSet rs= st.executeQuery();
                                while(rs.next()){
                                    int num=rs.getInt("questionNo");
                                    String qn= rs.getString("question");

                                    String insert="insert into challengeattempt(attemptNo,challengeNo,userName,questionNo,comment) VALUES(?,?,?,?,?)";
                                    try(PreparedStatement ps= con.prepareStatement(insert)){
                                        ps.setInt(1,attemptNo);
                                        ps.setString(2,challengeNumber);
                                        ps.setString(3,username);
                                        ps.setInt(4,num);
                                        ps.setString(5,result);
                                        ps.executeUpdate();

                                    }
                                }
                            }
                        } else if (ans.equals(null)) {
                            score = score;
                            result="wrong";
                            String q="Select question,questionNo FROM question join answer on answer.answerNo = question.questionNo WHERE answer ="+tableAns;
                            try(PreparedStatement st= con.prepareStatement(q)){
                                ResultSet rs= st.executeQuery();
                                while(rs.next()){
                                    int num=rs.getInt("questionNo");
                                    String qn= rs.getString("question");

                                    String insert="insert into challengeattempt(attemptNo,challengeNo,userName,questionNo,comment) VALUES(?,?,?,?,?)";
                                    try(PreparedStatement ps= con.prepareStatement(insert)){
                                        ps.setInt(1,attemptNo);
                                        ps.setString(2,challengeNumber);
                                        ps.setString(3,username);
                                        ps.setInt(4,num);
                                        ps.setString(5,result);
                                        ps.executeUpdate();

                                    }
                                }
                            }
                        } else {
                            score = score;
                        }


                }
            }



            // Update score in database
            String maQuery = "INSERT INTO challengescore (userName, email, challengeNo,attemptNo, score) VALUES (?, ?, ?, ? ,?)";
            try (PreparedStatement mastmt = con.prepareStatement(maQuery)) {
                mastmt.setString(1, username);
                mastmt.setString(2, email);
                mastmt.setString(3, challengeNumber);
                mastmt.setInt(4, attemptNo);
                mastmt.setInt(5, score);
                mastmt.executeUpdate();
            }


            pw.println(score);
        }

        private void sendConfirmationEmail(String regNo, Connection con) throws SQLException {
            // Query to get representative email based on registration number
            String query = "SELECT school_representative_name,school_representative_email FROM schools WHERE regNo = ?";
            try (PreparedStatement pstmt = con.prepareStatement(query)) {
                pstmt.setString(1, regNo);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    String repEmail = rs.getString("school_representative_email");
                    String repName = rs.getString("school_representative_name");
                    System.out.println(repEmail + repName);


                    // Send email
                    sendEmail(repEmail, "Pending Confirmation", "Dear "+ repName+" a new pupil has registered under your school");
                } else {
                    System.err.println("No representative found for registration number: " + regNo);
                }
            }
        }

        private void sendEmail(String to, String subject, String body) {

            Properties props = new Properties();
            props.put("mail.smtp.host", EmailConfig.SMTP_HOST);
            props.put("mail.smtp.port", EmailConfig.SMTP_PORT);
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            // Create a session with authentication
            Session session = Session.getInstance(props, new javax.mail.Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(EmailConfig.SMTP_USER, EmailConfig.SMTP_PASSWORD);
                }
            });


            try {
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(EmailConfig.SMTP_USER));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
                message.setSubject(subject);
                message.setText(body);

                Transport.send(message);

                System.out.println("Email sent successfully to " + to);
            } catch (MessagingException e) {
                e.printStackTrace();
            }

        }

        public static List<String> randomPick(List<String> questions, int numberOfQuestions) {
            Collections.shuffle(questions); // Shuffle the list
            int numToPick = Math.min(numberOfQuestions, questions.size());
            return questions.subList(0, numToPick);
        }
    }
}