package objects;

import java.io.IOException;

import cli.CLI;

import data.Bytes;
import data.FileManager;

/**
 * BPlusTree class
 * This class represents the entire BPlusTree and is responsible for inserting and getting a certain pointer
 * for a given key.
 * The BPlusTree header contains the following:
 * Attribute:		Stored @ position:
 * Block Size		0 - 3
 * Key size			4
 * Pointer size		5
 * Root ID			6-9
 * Root is leaf		10
 * @author Emil Bergwik
 *
 */
public class BPlusTree {
	private FileManager fm;
	private static byte[] headerBytes;
	static Node temporaryParent;
	private static int BLOCK_SIZE, ORDER, KEY_SIZE, POINTER_SIZE, ROOT_ID, ROOT_IS_LEAF;
	private int TREE_LEVEL;

	public BPlusTree(String fileName, int blockSize){
		fm = new FileManager(fileName, blockSize);
		try {
			if(fm.getSize() == 0){
				// The file opened was empty
				// Write new header data first
				// ROOT_IS_LEAF = 0: No. ROOT_IS_LEAF = 1: Yes.
				BLOCK_SIZE = blockSize;
				KEY_SIZE = 4;
				POINTER_SIZE = 4;
				ROOT_ID = 0;
				ROOT_IS_LEAF = 0;
				TREE_LEVEL = 1;
				ORDER = calculateOrder();
				if(ORDER < 3){
					System.out.println("The block size was calculated to less than 3, exiting.");
					System.exit(1);
				}
				headerBytes = new byte[BLOCK_SIZE];
				writeDataToHeader(headerBytes);
			}else{
				// Read header block and set info
				headerBytes = fm.read(0);
				readDataFromHeader(headerBytes);
				if(BLOCK_SIZE != blockSize){
					System.out.println("The block size contained in header block did not match input block size, exiting.");
					System.exit(1);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public void insert(int key, int value){
		try {
			headerBytes = fm.read(0);
			readDataFromHeader(headerBytes);
			if(ROOT_ID != 0){
				// There is a root, find it and start inserting from there
				if(ROOT_IS_LEAF == 1){
					// Root was leaf, just insert the key value.
					LeafNode root = new LeafNode(fm.read(ROOT_ID));
					root.insert(key, value, this);					
				}else{
					// Root is internal, find the correct leaf and write to it
					LeafNode node = get(key, ROOT_ID);
					node.insert(key, value, this);
				}
			}else{
				LeafNode root = new LeafNode(1);
				root.insert(key, value, this);
				this.setRoot(root.getID(), 1);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	public int getDiskPointer(int key){
		TREE_LEVEL = 0;
		return this.get(key, this.getRoot()).get(key);
	}
	/**
	 * Gets the LeafNode containing (or should contain) the key.
	 * Note: This method should be called with the root block as starting block.
	 * @param key - the key to search for
	 * @param blockID - the block ID to search in 
	 * @return the LeafNode that contains the key (or should contain the key)
	 */
	public LeafNode get(int key, int blockID){
		Node node = null;
		if(blockID == 0) return null;
		try {
			byte[] block = fm.read(blockID);
			boolean isLeaf = Bytes.byteToBoolean(block[4]);
			if(isLeaf){
				node = new LeafNode(block);
			}else{
				node = new InternalNode(block);
			}
			node.setParent(temporaryParent);
			temporaryParent = node;
			TREE_LEVEL++; // Used for calculation of tree height
			if(node.isLeaf()){
				temporaryParent = null;
				return (LeafNode) node;
			}else{
				node.log("Searching for key in " + node.getID());
				return get(key, ((InternalNode)node).get(key));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Sets the root of the BPlusTree
	 * @param rootID - the block ID of the root
	 * @param isLeaf - if the root is a leaf or not
	 */
	public void setRoot(int rootID, int isLeaf){
		try { 
			headerBytes = fm.read(0);
			ROOT_ID = rootID;
			ROOT_IS_LEAF = isLeaf;
			TREE_LEVEL++;
			Bytes.intToBytes(ROOT_ID, headerBytes, 6);
			headerBytes[10] = Bytes.intToByte(ROOT_IS_LEAF);
			writeDataToHeader(headerBytes);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * Reads the header block from the index file and returns the currently stored root block ID.
	 * @return the block containing the root.
	 */
	public int getRoot(){
		try {
			headerBytes = fm.read(0);
			readDataFromHeader(headerBytes);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ROOT_ID;
	}
	public static int getRootIsLeaf(){
		return ROOT_IS_LEAF;
	}
	public static int getOrder(){
		return ORDER;
	}
	public static int getKeySize(){
		return KEY_SIZE;
	}	
	public static int getPointerSize(){
		return POINTER_SIZE;
	}
	public static int getBlockSize(){
		return BLOCK_SIZE;
	}
	public int getTreeLevel(){
		return TREE_LEVEL;
	}
	public void resetTreeLevel(){
		this.TREE_LEVEL = 0;
	}
	public FileManager getFileManager(){
		return fm;
	}
	/**
	 * Sets the in-memory variables of the header from the byte array.
	 * @param headerBytes - the byte array to get the header information from
	 */
	private static void readDataFromHeader(byte[] headerBytes){
		BLOCK_SIZE = Bytes.bytesToInt(headerBytes, 0);
		KEY_SIZE = Bytes.byteToInt(headerBytes[4]);
		POINTER_SIZE = Bytes.byteToInt(headerBytes[5]);
		ROOT_ID = Bytes.bytesToInt(headerBytes, 6);
		ROOT_IS_LEAF = Bytes.byteToInt(headerBytes[10]);
		ORDER = calculateOrder();
	}
	/**
	 * Writes the data byte array to the header of the index file.
	 * @param headerBytes - the header byte to get the index information from
	 */
	private void writeDataToHeader(byte[] headerBytes){
		Bytes.intToBytes(BLOCK_SIZE, headerBytes, 0);
		headerBytes[4] = Bytes.intToByte(KEY_SIZE);
		headerBytes[5] = Bytes.intToByte(POINTER_SIZE);
		Bytes.intToBytes(ROOT_ID, headerBytes, 6);
		headerBytes[10] = Bytes.intToByte(ROOT_IS_LEAF);
		try {
			fm.write(headerBytes, 0);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/* Method for printing the information stored in the header */
	private void printHeader(){
		System.out.println("--- HEADER INFO ---");
		System.out.println("Block size: " + Bytes.bytesToInt(headerBytes,0));
		System.out.println("Key size: " + Bytes.byteToInt(headerBytes[4]));
		System.out.println("Pointer size: " + Bytes.byteToInt(headerBytes[5]));
		System.out.println("Root ID: " + Bytes.bytesToInt(headerBytes, 6));
		System.out.println("Root is Leaf: " + Bytes.byteToBoolean(headerBytes[10]));
		System.out.println("Tree Order: (not stored in header): " + ORDER);
		System.out.println("--- STOP HEADER INFO ---");
		System.out.flush();
	}
	/**
	 * Reads the header block of the index file and prints the stored data to console.
	 * @return - the string representation of the header block in the index file.
	 */
	public String readAndPrintHeaderToConsole(){
		try {
			headerBytes = fm.read(0);
			String result = "--- HEADER INFO ---\n";
			result += "Block size: " + Bytes.bytesToInt(headerBytes,0) + "\n";
			result += "Key size: " + Bytes.byteToInt(headerBytes[4]) + "\n";
			result += "Pointer size: " + Bytes.byteToInt(headerBytes[5]) + "\n";
			result += "Root ID: " + Bytes.bytesToInt(headerBytes, 6) + "\n";
			result += "Root is Leaf: " + Bytes.byteToBoolean(headerBytes[10]) + "\n";
			result += "Tree Order: (not stored in header): " + ORDER + "\n";
			result += "--- STOP HEADER INFO ---" + "\n";
			return result;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Calculate the order of the tree 
	 * The current implementation assumes that each block stores node ID (4 bytes), IsLeafByte (1 byte) and
	 * parent block ID (4 bytes) = 9 bytes of header information
	 * @return the order of the tree
	 */
	private static int calculateOrder(){
		return (BLOCK_SIZE - 9 - POINTER_SIZE)/(KEY_SIZE + POINTER_SIZE);
	}
	/**
	 * Dumps the data stored in the index file to the console.
	 */
	public void dumpIndex(){
		int blockNumber = 0;
		CLI.log("Dumping index file to console.. \n");
		while(blockNumber < fm.getSize()){
			try {
				byte[] data = fm.read(blockNumber);
				if(blockNumber == 0){
					readDataFromHeader(data);
					printHeader();
				}else{
					boolean isLeaf = Bytes.byteToBoolean(data[4]);
					if(isLeaf){
						LeafNode leaf = new LeafNode(data);
						leaf.printKeyDiskPointers();
					}else{
						InternalNode internal = new InternalNode(data);
						internal.printKeyDiskPointers();
					}
				}
				blockNumber++;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
		}
	}
}