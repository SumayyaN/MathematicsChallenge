package src;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class challengeServer {
	
	public static void main(String[] args) {
		ServerSocket ss = null;
		Socket soc = null;
		BufferedReader br = null;
		PrintWriter pw = null;
		Connection con = null;
		BufferedReader reader = null;
		BufferedReader answers = null;

		try {
			// let us connect to the database
			con = DriverManager.getConnection("jdbc:mysql://localhost:3306/mathchallenge", "root", "");
			System.out.println("Waiting...");
			ss = new ServerSocket(8888);
			soc = ss.accept();
			System.out.println("Connection Established...");

			// reading the requests from the client
			pw = new PrintWriter(soc.getOutputStream(), true);
			br = new BufferedReader(new InputStreamReader(soc.getInputStream()));
			String[] credentials = br.readLine().split(",");
			String username = credentials[0];
			String email = credentials[1];

			// querying the database for existance of these details in the acceptedapplicant
			// table

			String query = "SELECT username from accepted WHERE username = ?";

			try (PreparedStatement state = con.prepareStatement(query)) {
				state.setString(1, username);
				ResultSet rs = state.executeQuery();

				if (rs.next()) {
					String result = rs.getString("username");

					if (username.equalsIgnoreCase(result)) {
						String que = "SELECT email FROM accepted WHERE email = ?";

						try (PreparedStatement stmt = con.prepareStatement(que)) {
							stmt.setString(1, email);
							ResultSet eme = stmt.executeQuery();

							if (eme.next()) {
								String effect = eme.getString("email");

								if (email.equalsIgnoreCase(effect)) {

									// code to retrieve all the challenges that this guy selected during
									// registration
									String challChoice = "SELECT challengeName, challengeID FROM accepted WHERE userName = ?";
									try (PreparedStatement ps = con.prepareStatement(challChoice)) {
										ps.setString(1, username);

										ResultSet choice = ps.executeQuery();
										List<String> yourChallenge = new ArrayList<>();

										while (choice.next()) {
											String chaName = choice.getString("challengeName");
											String chaId = choice.getString("challengeID");

											yourChallenge.add(chaName + "\t" + chaId);
										}
										// pw = new PrintWriter(soc.getOutputStream(), true);
										pw.println("Success");
										for (String x : yourChallenge) {
											// System.out.println(x);
											pw.println(x);
										}
									}
								}
							} else {
								pw.println("check and correct your details");
							}
						}
					}
				} else {
					// instruct him/her to enter the register command to do so or exit
					pw.println("Check your credentials or register with us please!");

				}
				pw.println("END");
				// challenge to attempt well retrieved from the client
				reader = new BufferedReader(new InputStreamReader(soc.getInputStream()));
				String[] atemChoice = reader.readLine().split(" ");

				//int challTdNo = Integer.parseInt(atemChoice[1]);

				if (atemChoice[0].equalsIgnoreCase("attemptChallenge")) {
					String challengeNumber = atemChoice[1];
					// System.out.println(challengeNumber);
					// check against the challenge table
					String chalquery = "SELECT ChallengeId FROM challenge Where challengeId=?";
					PreparedStatement psmt = con.prepareStatement(chalquery);
					psmt.setString(1, challengeNumber);
					ResultSet cs = psmt.executeQuery();
					while (cs.next()) {
						String match = cs.getString("challengeId");
						if (challengeNumber.equalsIgnoreCase(match)) {// access the table with the challenge questions
							String challengeTable = "SELECT question,answer,marks FROM Table" + challengeNumber;

							try (PreparedStatement que = con.prepareStatement(challengeTable)) {
								;
								ResultSet chalDetails = que.executeQuery();
								// System.out.println("Am in here");
								List<String> questions = new ArrayList<>();// list to store the questions

								String question = "";
								String answer = "";
								int marks = 0;
								while (chalDetails.next()) {
									question = chalDetails.getString("question");
								//	answer = chalDetails.getString("answer");
									marks = chalDetails.getInt("marks");
									questions.add(question + " "+  "("+"mark " +marks+")");

								}

						//		System.out.println(questions);

								List<String> randomQuestions = randomPick(questions, 10);
								for (String myquestion : randomQuestions) {
									// System.out.println("am actually sending but the prob is not with me");
									pw.println(myquestion);

								}
								pw.println("DONE");
								//so i gat to read answers from the pupil and at the same take in each answer and accumulate marks for this niggae/
								
								//Tryna set a timer for the method below
								//long startTime = System.currentTimeMillis();
								//long endTime = startTime + (60000);
								
								//while(System.currentTimeMillis() < endTime) {//timer STarts here
								int score = 0;
								for(int i = 0; i < 10; i++) {//Just represents the number of qns or times am gonna have to pick the answers
									answers = new BufferedReader(new InputStreamReader(soc.getInputStream()));
									String ans = answers.readLine();
									
									//for Each answer entered, i check it up whether its correct from the table challengeno specified
									
									String ansQuery = "SELECT answer FROM Table"+ challengeNumber +" WHERE answer = ?";
								
									try(PreparedStatement ansStmt = con.prepareStatement(ansQuery)){
										ansStmt.setString(1, ans);
										
										ResultSet ansResult = ansStmt.executeQuery();
										//getting correct answer from the table
										
										//System.out.println("i cannt"); testing
										String tableAns = "";
										if (ansResult.next()) {
											 tableAns = ansResult.getString("answer");
											//System.out.println(tableAns);
											//checking whether answer is correct
											if(ans.equalsIgnoreCase(tableAns)) {
												String markQuery = "SELECT marks FROM Table"+ challengeNumber +" WHERE answer = ?";
												
												try(PreparedStatement markStmt = con.prepareStatement(markQuery)){
													markStmt.setString(1, tableAns);
													
													 //
													ResultSet markset = markStmt.executeQuery();
													while(markset.next()) {
														int x = markset.getInt("marks");
													    score += x;
													}
													
												}
											}
											//what should we do if the ans is wrong means elseif and the notsure see you tomorrow
										}else if(!(ans.equalsIgnoreCase(tableAns)) && !(ans.equals("-"))){
											score -= 3;
										}else if(ans.equals("-")) {
											score = score;
										}else {
											score = score;
										}
									}
									}
								
								//System.out.println("YOUR SCORE: "+score +" marks");testing
								pw.println(score);
								//}//timerENds here
							}
						} // end of if

					}

				}//else if(atemChoice[0].equalsIgnoreCase("Exit")) {
					//return;
				//}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
			try {
				if (reader != null)
					reader.close();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
			try {
				if (answers != null)
					answers.close();
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
			try {
				if (con != null)
					con.close();
			} catch (SQLException sqle) {
				sqle.printStackTrace();
			}

		}

	}// end of main

	public static List<String> randomPick(List<String> questions, int numberOfQuestions) {
		Collections.shuffle(questions); // Shuffle the list

		// Ensure we don't try to pick more questions than available
		int numToPick = Math.min(numberOfQuestions, questions.size());

		// Return sublist of first 'numToPick' questions
		return questions.subList(0, numToPick);
	}
}