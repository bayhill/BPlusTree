package cli;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Scanner;

import data.RecordManager;

import objects.BPlusTree;
/**
 * This is a very basic command line interface for testing the BPlusTree implementation.
 * @author Emil Bergwik
 *
 */
public class CLI {
	public static FileWriter fw;
	public static PrintWriter pw;
	public static String VERBOSITY = "none", INDEX_FILE_NAME = "index.dat", DATABASE_FILE_NAME="database.dat";
	private static BPlusTree tree;
	private static HashSet<Integer> storedKeys;

	private static Scanner sc;

	public static void main(String[] args) throws IOException {
		sc = new Scanner(System.in);
		String logFile = "logs/log_" + 
		new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime()) + ".txt";
		fw = new FileWriter(logFile);
		pw = new PrintWriter(fw);
		log("---PROGRAM START---");
		showMainMenu();		
	}
	/* Main menu for the command line interface */
	public static void showMainMenu(){
		while(true){
			log("-----------------");
			log("Main menu");
			log("[C]lear tree (delete index file)\n" +
					"[D]ump index to console\n"+
					"[G]enerate random index BPT\n" +
					"[H]eight of tree\n" +
					"[I]nsert\n" +
					"I[M]port index\n" +
					"[Q]uery\n" +
					"[R]ead database table from file\n"+
					"[E]xit"
			);
			log("-----------------");
			String input = sc.nextLine();
			char in = input.charAt(0);
			switch(in){
			case 'C': clearAndDelete();
			break;
			case 'D': dumpIndex();
			break;
			case 'G': generateRandomTree();
			break;
			case 'I': insert();
			break;
			case 'M': readIndexFromFile();
			break;
			case 'Q': queryTree();
			break;
			case 'R': readDatabaseFromFile();
			break;
			case 'H': height();
			break;
			case 'E': exit();
			default:
				showMainMenu();
			}
		}		
	}
	
	/* Reads a database table stored on secondary storage and fills the BPT with those values */
	private static void readDatabaseFromFile() {
		if(tree == null){
			log("No tree available, creating new tree.");
			log("Block size:");
			int blockSize = Integer.parseInt(sc.nextLine());
			if(blockSize >= 38){
				tree = new BPlusTree(INDEX_FILE_NAME, blockSize);
			}else{
				log("Block size cannot be lower than 38 bytes.");
				return;
			}
		}
		log("Amount of keys to read from database table:");
		int amount = Integer.parseInt(sc.nextLine());
		if(amount > 53000){
			log("Table contains only 53000 keys, setting amount to 53000");
			amount = 53000;
		}
		log("Reading database table...");
		try {
			storedKeys = RecordManager.loadDatabaseIntoTree(tree, DATABASE_FILE_NAME, amount);
		} catch (IOException e) {
			e.printStackTrace();
		}
		log("Done.");
	}
	
	/* Deletes the tree from main memory and secondary storage */
	private static void clearAndDelete() {
		if(tree != null){
			log("Deleted index file: "+tree.getFileManager().deleteFile());
			tree = null;
		}else{
			log("No tree available, returning to main menu.");
		}
	}
	
	/* Gets the height of the current BPT */
	private static void height() {
		if(tree != null && storedKeys != null){
			tree.resetTreeLevel();
			Iterator<Integer> it = storedKeys.iterator();
			tree.getDiskPointer(it.next());
			log("Tree height: " + tree.getTreeLevel());
		}
	}
	
	/* Reads an existing index file from the file system */
	private static void readIndexFromFile() {
		int blockSize=0;
		if(tree == null){
			log("Specify block size of the to be read index file:");
			blockSize = Integer.parseInt(sc.nextLine());
		}
		if(blockSize < 38){
			log("Block size cannot be lower than 38 bytes. Setting block size to 38 bytes.");
			blockSize = 38;
		}else{
			tree = new BPlusTree(INDEX_FILE_NAME, blockSize);
		}
		log("Done.");
	}
	
	/* Dumps the index to console */
	public static void dumpIndex(){
		if(tree == null){
			log("No index tree available, returning to main menu..");
			return;
		}else{
			String lastVerbosity = VERBOSITY;
			VERBOSITY = "all";
			tree.dumpIndex();
			VERBOSITY = lastVerbosity;
		}
	}
	
	/* Generates a BPT containing a specified amount of random key,value pairs */
	public static void generateRandomTree(){
		storedKeys = new HashSet<Integer>();
		if(tree == null){
			log("No tree available, creating new tree.");
			log("Block size:");
			int blockSize = Integer.parseInt(sc.nextLine());
			if(blockSize >= 38){
				tree = new BPlusTree(INDEX_FILE_NAME, blockSize);
			}else{
				log("Block size lower than 38 bytes not allowed. Setting block size to 38 bytes.");
				blockSize = 38;
				tree = new BPlusTree(INDEX_FILE_NAME, blockSize);
			}
		}
		log("Amount of key, value pairs to insert:");
		int amount = Integer.parseInt(sc.nextLine());
		while(storedKeys.size() < amount){
			storedKeys.add((int)Math.round(Math.random() * 89999) + 10000);
		}
		Iterator<Integer> it = storedKeys.iterator();
		while(it.hasNext()){
			int keyvalue = it.next();
			tree.insert(keyvalue, keyvalue);
		}
		log("Done.");
	}

	/* Inserts a user-specified key, value pair into the BPT */
	public static void insert(){
		if(tree==null){
			log("No tree available, creating new tree.");
			log("Block size:");
			int blockSize = Integer.parseInt(sc.nextLine());
			if(blockSize >= 38){
				tree = new BPlusTree(INDEX_FILE_NAME, blockSize);
			}else{
				log("Block size cannot be lower than 38 bytes.");
				return;
			}
		}
		log("Key to insert:");
		int key = Integer.parseInt(sc.nextLine());
		tree.insert(key, key);
		if(storedKeys == null) storedKeys = new HashSet<Integer>();
		storedKeys.add(key);
		log("Done.");
	}

	/* Queries the BPT for the pointer associated with a user-specified key */
	public static void queryTree(){
		if(tree == null){
			log("No tree available to search in, returning to main menu..");
			return;
		}
		tree.getFileManager().resetReadWriteCounter();
		log("Key to query:");	
		int key = Integer.parseInt(sc.nextLine());
		long start = System.currentTimeMillis();
		int result = tree.getDiskPointer(key);
		long stop = System.currentTimeMillis();
		log("Query Result:");
		log("Queried key: " + key );
		if(result != 0){
			log("Block pointer: " + result);
		}else{
			log("The key does not exist in the current BPlusTree.");
		}
		log("Amount of disk accesses: " + tree.getFileManager().getNumberOfReadWrites());
		long execution = stop - start;
		log("Query time: " + execution + " ms");
	}
	
	/* Exits the command line interface program  */
	public static void exit(){
		log("---PROGRAM EXIT---");
		pw.close();
		System.exit(0);
	}

	/* Helper method for printing a message to system out and the specified log file */
	public static void log(String message){
		pw.println(message);
		System.out.println(message);
		System.out.flush();
	}
}
