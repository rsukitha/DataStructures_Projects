import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class TrieArticle {

	static TrieNode root = new TrieNode();
	static HashMap<String, Integer> companyCount = new HashMap<>();
	static int WordCount = 0;
	static int Companies = 0;

	public static void main(String[] args) {

		ArrayList<String> lines = readCompanies(); //Read from file

		if(lines == null || lines.size()==0) {
			System.out.println("File error. Exiting!");
			return;
		}
		
		processCompanies(lines); //put files in the tree

		boolean flag = true;
		boolean answerPrinted = false;
		System.out.println("Read " + Companies + " companies and their aliases from file.");
		System.out.println("Enter the article lines below. Program will exit when a line with only periods is input.");
		while (flag) {
			Scanner scan = new Scanner(System.in);
			String input = scan.nextLine();
			boolean isValid = false;
			for (int i = 0; i < input.length(); i++) {
				if (input.charAt(i) != '.')
					isValid = true;
			}
			if (isValid) {
				searchTrie(input);
				// System.out.println("1..." + input);
			} else {
				answerPrinted = true;
				printAnswer();
				flag = false;
			}
		}

		if (!answerPrinted) {
			printAnswer();
		}

	}

	//searches the text in the trie
	private static void searchTrie(String input) {
		TrieNode currNode = root;
		boolean isAdded = false;

		for (int i = 0; i < input.length(); i++) {
			Character letter = input.charAt(i);
			currNode = currNode.children.get(letter);
			if (currNode == null) {
				currNode = root;
				isAdded = false;
			} else {
				if (currNode.mainName != null) {

					if (isAdded == false) {
						Integer cnt = companyCount.get(currNode.mainName);
						if (cnt == null)
							companyCount.put(currNode.mainName, 1);
						else
							companyCount.put(currNode.mainName, cnt + 1);
						isAdded = true;
					}
					WordCount += currNode.articlesLength;

				} else if (isAdded && i + 1 < input.length() && currNode.children.get(input.charAt(i + 1)) == null) {
					currNode = root;
					isAdded = false;
				}
			}
		}

		String words[] = input.split("\\s+");

		WordCount += words.length;
		for (String word : words) {
			if (word.equals("an") || word.equals("and") || word.equals("the") || word.equals("but") || word.equals("a")
					|| word.equals("or")) {
				WordCount--;
			}
		}

	}

	//prints answer as per tabular format required.
	private static void printAnswer() {
		float totalcompanywords = 0;
		float relevance;
		System.out.println("Company                       " + "  " + "Hit Count" + "  " + "Relevance");
		while (companyCount.size() > 0) {
			String maxKey = null;
			float maxCount = -1;
			for (String key : companyCount.keySet()) {
				if (companyCount.get(key) > maxCount) {
					maxCount = companyCount.get(key);
					maxKey = key;
				}
			}

			companyCount.remove(maxKey);

			relevance = (float) maxCount / WordCount * 100;
			System.out.printf("%-30s%2s%-9d%2s%6.4f%s%n", maxKey, "  ", (int) maxCount, "  ", relevance, "%");
			totalcompanywords += maxCount;
		}
		relevance = totalcompanywords / WordCount * 100;
		System.out.print("Total                           ");
		System.out.printf("%-9d%2s%6.4f%s%n", (int) totalcompanywords, "  ", relevance, "%");

		System.out.println("Total Words                     " + WordCount);
	}

	//extracts line by line from array of strings and splits by tab before adding to trie
	private static void processCompanies(ArrayList<String> lines) {
		int count = 0;

		while (count < lines.size()) {
			String line = lines.get(count);

			String companies[] = line.split("\t");
			String mainName = companies[0];

			addToTrie(companies, mainName);
			count++;
		}
		Companies = count;

	}

	//adds company names to trie
	private static void addToTrie(String[] companies, String mainName) {
		int nameCount = 0;

		while (nameCount < companies.length) {
			int letterCount = 0;
			TrieNode currNode = root;
			TrieNode prevNode = null;

			String words[] = companies[nameCount].split("\\s+");
			int articles = 0;
			for (String word : words) {
				if (word.equals("an") || word.equals("and") || word.equals("the") || word.equals("but")
						|| word.equals("a") || word.equals("or")) {
					articles++;
				}
			}

			while (letterCount < companies[nameCount].length()) {
				prevNode = currNode;
				Character letter = companies[nameCount].charAt(letterCount);
				currNode = currNode.children.get(letter);
				if (currNode == null) {
					currNode = new TrieNode();
					currNode.letter = letter;
					prevNode.children.put(letter, currNode);
				}
				letterCount++;
			}
			currNode.mainName = mainName;
			currNode.articlesLength = articles;
			nameCount++;
		}
	}

	//reads company names and aliases from companies.dat file
	private static ArrayList<String> readCompanies() {
		ArrayList<String> lines = new ArrayList<String>();
		try {
			FileReader fr = new FileReader("companies.dat");
			BufferedReader br = new BufferedReader(fr);
			String line;

			while ((line = br.readLine()) != null) {
				lines.add(line);
			}

			br.close();
			return lines;
		} catch (Exception e) {
			return null;
		}
	}

}


//Trie Node class
class TrieNode {
	Character letter;
	HashMap<Character, TrieNode> children = new HashMap<>();
	String mainName = null;
	int articlesLength = 0;
}
