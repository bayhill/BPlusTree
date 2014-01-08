package objects;

import java.nio.ByteBuffer;
import java.util.Arrays;

import cli.CLI;

import data.Bytes;

/**
 * Abstract class Node containing variables that are not unique to either LeafNode or InternalNode.
 * A Node contains byte array storage containers for the header, keys and pointers of the node as described below.

 * Node contains:
 * Attribute:			Stored @ byte:
 * ID 					0 to 3
 * isLeafByte			4
 * RightLeaf (for Leaf) 5 - 8 (NOT USED SINCE VERSION 1.4)
 * keys 	 			9 to 9 + KEY_SIZE * (ORDER-1)+KEY_SIZE
 * pointers 			10 + KEY_SIZE*(ORDER-1)+KEY_SIZE to 
 * 						10 + KEY_SIZE*(ORDER-1)+KEY_SIZE + POINTER_SIZE*ORDER+POINTER_SIZE
 * 
 * A Node is considered a root node if its parent reference is null (set in main memory)
 * 
 * Example:
 * key = 4
 * pointer = 4
 * block = 64
 * order = (40 - 9 - 4)/(4 + 4) = 51/8 = 3 (# of keys)
 * keys = [9,21]
 * pointers = [22,38]
 * 
 * @author Emil Bergwik
 */
public abstract class Node{

	protected byte[] header;
	protected byte[] keys;
	protected byte[] pointers;
	
	Node parent;
/**
 * Base constructor for Node.
 * @param ID - the ID of the Node (i.e the block number storing the Node)
 */
	public Node(int ID){
		this.header = new byte[9];
		Bytes.intToBytes(ID, header, 0);
	}
/**
 * Constructor for Node
 * @param ID - - the ID of the Node (i.e the block number storing the Node)
 * @param isLeaf - true if the Node is a LeafNode, else false
 */
	public Node(int ID, boolean isLeaf){
		this(ID);
		this.header[4] = Bytes.booleanToByte(isLeaf);
	}
/**
 * Constructor for a node that has been read from {@code RandomAccessFile}. Each subclass uses this constructor in its own way
 * @param data
 */
	public Node(){}

	public int getID(){
		return Bytes.bytesToInt(this.header, 0);
	}

	public boolean isLeaf(){
		return Bytes.byteToBoolean(this.header[4]);
	}
	
	public void setParent(Node parent){
		if(parent != null){
			if(this.isLeaf()){
				this.log("Set parent " + parent.getID() + " for LeafNode " + this.getID());
			}else{
				this.log("Set parent " + parent.getID() + " for InternalNode " + this.getID());
			}
			this.parent = parent;
		}else{
		}
	}
	public int getParent(){
		if(this.parent != null ) return this.parent.getID();
		return 0;
	}

	public byte[] getKeys(){
		return this.keys;
	}

	public void setKeys(byte[] keys){
		if(keys.length > this.keys.length+BPlusTree.getKeySize()){
			throw new IndexOutOfBoundsException("Tried to set keys with size " + keys.length + ".");
		}
		this.keys = keys;
	}

	public byte[] getPointers(){
		return this.pointers;
	}

	public void setPointers(byte[] pointers){
		if(pointers.length > this.pointers.length+BPlusTree.getPointerSize()){
			// pointer byte array larger than what is allowed
			throw new IndexOutOfBoundsException("Tried to set pointers with size " + pointers.length + ".");
		}
		this.pointers = pointers;
	}

	public boolean isRoot(){
		return this.parent == null;
	}

	public int getOrder(){
		return BPlusTree.getOrder();
	}
/**
 * Method used for converting the Node data object to a byte array for storage to {@code RandomAccessFile}.
 * @return the byte representation of the header, keys and pointers of this node
 */
	public byte[] toBytes(){
		ByteBuffer bb = ByteBuffer.allocate(BPlusTree.getBlockSize());
		bb.put(this.header);
		// Store the keys except for the last four extra bytes
		for(int i=0;i<this.keys.length-4;i+=4){
			bb.putInt(Bytes.bytesToInt(this.keys, i));
		}
		// Store the pointers except for the last four extra bytes
		for(int i=0;i<this.pointers.length-4;i+=4){
			bb.putInt(Bytes.bytesToInt(this.pointers, i));
		}
		byte[] result = bb.array();
		bb.clear();
		return result;
	}
/**
 * General log function for writing important messages to console and log file in 
 * the correct order
 * @param message the message to write
 * @see CLI
 */
	public void log(String message){
		if(CLI.VERBOSITY != "none"){
			System.out.println(message);
			CLI.pw.println(message);
			System.out.flush();
		}
	}
}