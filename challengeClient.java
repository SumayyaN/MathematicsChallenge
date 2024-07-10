package src;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.sql.SQLException;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.List;

public class challengeClient {
	public static void main(String[] args) throws SocketException {
		Socket soc = null;
		BufferedReader br = null;
		BufferedReader fromSoc = null;
		// BufferedReader ff = null;
		PrintWriter pw = null;

		try {
			System.out.println("Loading...");
			soc = new Socket("localhost", 8888);

			// Display some sort of impressive welcome message
			System.out.println("\nWELCOME TO THE CHALLENGE WINDOW!\nPlease enter your details to proceed");

			// we get to read the details of the user ie the username and email and send
			// them to the server
			br = new BufferedReader(new InputStreamReader(System.in));
			System.out.print("Username: ");
			String name = br.readLine();
			System.out.print("Email: ");
			String email = br.readLine();
			String details = name + "," + email;

			pw = new PrintWriter(soc.getOutputStream(), true);
			pw.println(details);

			// ff = new BufferedReader(new InputStreamReader(soc.getInputStream()));
			fromSoc = new BufferedReader(new InputStreamReader(soc.getInputStream()));

			if (fromSoc.readLine().equalsIgnoreCase("success")) {
				System.out.println("\nEnter the following commands to proceed:\n a) viewChallenges\n b)Exit\n");

				String input = br.readLine();

				if (input.equalsIgnoreCase("viewChallenges")) {
					List<String> choice = new ArrayList<>();

					String yourChoice = "";

					while (!(yourChoice = fromSoc.readLine()).equalsIgnoreCase("END")) {

						choice.add(yourChoice);

					}

					System.out.println("Your Choice:");

					for (String j : choice) {
						System.out.println(j);
					}

					System.out.println();
					System.out.println(
							"In order to Attempt challenge, Enter this command: attemptChallenge <challengeNumber> or exit to leave\n");
					System.out.println("       ****INSTRUCTIONS*\n  ->Enter your Answer on the next line just after the question\n  ->Enter - for notsure and it earns 0 marks\n  ->Correct answer earns you allocated marks\n  ->Wrong Answer earns you -3 marks\n  ->Choose wisely, let justice prevail\n");

					String attemptChoice = br.readLine();
					pw.println(attemptChoice);

					// recieving questions from the Server
					List<String> question = new ArrayList<>();
                    //instructions
					String qn = "";

					while (!(qn = fromSoc.readLine()).equalsIgnoreCase("done")) {
						question.add(qn);

					}
					System.out.println();
					

					System.out.print("COUNT DOWN STARTS NOW!");
					for (String quiz : question) {
						System.out.println("\n" + quiz);

						// receiving the answer per question
						String ans = br.readLine();
						// System.out.println(ans);

						pw.println(ans);

					}
                  //printing out your score in that particular challenge
					System.out.println("\n  					 			SCORE: "+ fromSoc.readLine());
				}
			} else {
				System.out.println("Check your credentials and try again or register!");

			}

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