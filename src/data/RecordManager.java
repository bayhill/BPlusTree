package data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

import objects.BPlusTree;


/**
 * RecordManager class for:
 *  * Generating a new B+ tree index given a database file,
 *  * Generating a new database file given a source file containing names.
 * @author Emil Bergwik
 *
 */
public class RecordManager {

	private static BufferedReader br;
	private static BufferedWriter bw;
	private static HashSet<Integer> storedKeys; // Stores the keys inserted into the tree
/**
 * Reads a database file and inserts every row within the database
 * as a new key, pointer value into the tree. Records within the database is expected
 * to be on the format:
 * StudentID (Integer);FirstName LastName (String);MajorCode (Integer);Sex (String)
 * @param tree - the tree which to insert values into
 * @param fileName - the database file to load records from
 * @throws IOException if the underlying reader has an I/O error. 
 */
	public static void loadDatabaseIntoTree(BPlusTree tree, String fileName) throws IOException{
		br = new BufferedReader(new FileReader(fileName));
		String line = br.readLine();
		while (line != null) {
			String[] record = line.split(";");
			if(record[0] != null){
				tree.insert(Integer.parseInt(record[0]), 1);				
			}
			line = br.readLine();
		}
	}
	
	/**
	 * Reads a database file line by line and inserts them into a BPlusTree
	 * @param tree - the BPlusTree to load into the database
	 * @param fileName - the file name to read the database from
	 * @param amount - the amount of records to read from the file
	 * @return the HashSet containing the unique values inserted into the file
	 * @throws IOException if the tree is not initialized or if any IOException occurs during reading the file.
	 */
	public static HashSet<Integer> loadDatabaseIntoTree(BPlusTree tree, String fileName, int amount) throws IOException{
		if(tree == null) throw new IOException();
		storedKeys = new HashSet<Integer>();
		int order = tree.getOrder();
		int blockPointer = 0;
		int counter = 0;
		br = new BufferedReader(new FileReader(fileName));
		String line = br.readLine();
		while (line != null && counter < amount) {
			String[] record = line.split(";");
			if(record[0] != null){
				tree.insert(Integer.parseInt(record[0]), Integer.parseInt(record[0]));
				storedKeys.add(Integer.parseInt(record[0]));
				if(counter != 0 && counter % order == 0) blockPointer++;
			}
			counter++;
			line = br.readLine();
		}
		return storedKeys;
	}
/**
 * Randomizing a new database file given a list of first and last names
 * The method randomizes tuples according to the following format:
 * StudentID (Integer); FirstName LastName (String); MajorCode (Integer); Sex (String)
 * @param in - the input file containing first and last names
 * @param out - the output database file to write to
 * @return the amount of generated tuples
 * @throws IOException if any of the underlying streams has an I/O error
 */
	public static HashSet<Integer> randomizeNewDatabase(String in, String out) throws IOException{
		storedKeys = new HashSet<Integer>();
		br = new BufferedReader(new FileReader(in));
		bw = new BufferedWriter(new FileWriter(out));
		int generatedAmount = 0;
		Random rnd = new Random();
		String name = br.readLine();
		while(storedKeys.size() < 60000){
			storedKeys.add((int)Math.round(Math.random() * 89999) + 10000);
		}
		Iterator<Integer> it = storedKeys.iterator();
		while (name != null && it.hasNext()) {
			int studentID = it.next();
			int majorCode = rnd.nextInt(10);
			String sex;
			if(rnd.nextBoolean()){
				sex = "M";
			}else{
				sex = "F";
			}
			bw.write(studentID + ";" + name + ";" + majorCode + ";" + sex);
			bw.newLine();
			generatedAmount++;
			name = br.readLine();
		}
		return storedKeys;
	}
	
	
	/**
	 * Closes the underlying streams if they are open.
	 * @throws IOException if any of the streams has an I/O error.
	 */
	public static void close() throws IOException{
		if(bw != null){
			bw.close();
		}
		if(bw != null){
			bw.close();
		}
	}
}
