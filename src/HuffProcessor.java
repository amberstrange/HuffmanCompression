import java.util.PriorityQueue;

/**
 *	Interface that all compression suites must implement. That is they must be
 *	able to compress a file and also reverse/decompress that process.
 * 
 *	@author Brian Lavallee
 *	@since 5 November 2015
 *  @author Owen Atrachan
 *  @since December 1, 2016
 */
public class HuffProcessor {

	public static final int BITS_PER_WORD = 8;
	public static final int BITS_PER_INT = 32;
	public static final int ALPH_SIZE = (1 << BITS_PER_WORD); // or 256
	public static final int PSEUDO_EOF = ALPH_SIZE;
	public static final int HUFF_NUMBER = 0xface8200;
	public static final int HUFF_TREE  = HUFF_NUMBER | 1;
	public static final int HUFF_COUNTS = HUFF_NUMBER | 2;

	public enum Header{TREE_HEADER, COUNT_HEADER};
	public Header myHeader = Header.TREE_HEADER;
	
	/**
	 * Compresses a file. Process must be reversible and loss-less.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be compressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	
	public void compress(BitInputStream in, BitOutputStream out){
	    int[] counts = readForCounts(in);
	    TreeNode root = makeTreeFromCounts(counts);
	    String[] codings = makeCodingsFromTree(root);
	 
	    writeHeader(root,out);
	 
	    in.reset();


	    writeCompressedBits(in,codings,out);
	}
	
	public int[] readForCounts(BitInputStream in){
		int[] answer =new int[256];
		while(true){
			int bit = in.readBits(BITS_PER_WORD);
			if(bit==-1) break;
			answer[bit]++;
		}
		return answer;
	}
	public TreeNode makeTreeFromCounts(int[] freq){
		PriorityQueue<TreeNode> pq = new PriorityQueue<TreeNode>();
		for(int i =0; i<256; i++){
			if(freq[i]!=0){
				pq.add(new TreeNode(i, freq[i]));
			}
		}
		pq.add(new TreeNode(PSEUDO_EOF,1));
		while(pq.size()>1){
			TreeNode left = pq.remove();
			TreeNode right = pq.remove();
			TreeNode t = new TreeNode(-1, left.myWeight + right.myWeight, left, right);
			pq.add(t);
		}
		TreeNode root = pq.remove();
		return root;
	}
	public boolean isLeafNode(TreeNode t){
		return (t.myValue>=0&&t!=null);
	}
	public String[] makeCodingsFromTree(TreeNode t){
		String[] answer = new String[257];
		makeCodingsHelper(t, "", answer);
		return answer;
		
	}
	public void makeCodingsHelper(TreeNode root, String traversal, String[]codings ){
		if(isLeafNode(root)){
			codings[root.myValue]=traversal;
			return;
		}
		if(root.myLeft!=null){
			makeCodingsHelper(root.myLeft, traversal + "0", codings);
		}
		if(root.myRight!=null){
			makeCodingsHelper(root.myRight, traversal + "1", codings);
		}
		
	}
	public void writeHeader(TreeNode t,BitOutputStream out){
		out.writeBits(32, HUFF_TREE);
		writeTree(t, out);
	}
	public void writeTree(TreeNode t, BitOutputStream out){
		if(isLeafNode(t)){
			out.writeBits(1,1);
			out.writeBits(9, t.myValue);
			return;
		}
		out.writeBits(1, 0);
		if(t.myLeft!=null){
			writeTree(t.myLeft, out);
		}
		if(t.myRight!=null){
			writeTree(t.myRight, out);
		}
	}
	public void writeCompressedBits(BitInputStream in, String[] codings, BitOutputStream out){
		int bit = 0;
		while((bit= in.readBits(BITS_PER_WORD))!= -1){
			
			String encode = codings[bit];
			out.writeBits(encode.length(), Integer.parseInt(encode, 2));
			
		}
		out.writeBits(codings[PSEUDO_EOF].length(), Integer.parseInt(codings[PSEUDO_EOF], 2));
	}
	
	/**
	 * Decompresses a file. Output file must be identical bit-by-bit to the
	 * original.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be decompressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	
	public void decompress(BitInputStream in, BitOutputStream out){
	    int id = in.readBits(BITS_PER_INT);
	    // check id to see if valid compressed file
	    if(id!=HUFF_TREE) throw new HuffException("not correct file");


	    TreeNode root = readTreeHeader(in);
	    readCompressedBits(root,in,out);
	}
	public TreeNode readTreeHeader(BitInputStream in){
		
		if(in.readBits(1)==0){
			TreeNode left = readTreeHeader(in);
			TreeNode right = readTreeHeader(in);
			return new TreeNode(0,0, left, right);
		}
		else{
			return new TreeNode(in.readBits(9),-1);
			}
	}
	public void readCompressedBits(TreeNode root, BitInputStream in, BitOutputStream out){
		TreeNode current = root;
		int bit = 0;
		while((bit=in.readBits(1))!=-1){
			
			if(current.myLeft==null && current.myRight==null){
				if(current.myValue==PSEUDO_EOF) break;
				out.writeBits(8, current.myValue);
				current = root;
				
				
				}
			if(bit==0){
				current=current.myLeft;
			}
			else{
				current= current.myRight;
			}
			}
		}
	
	
	public void setHeader(Header header) {
        myHeader = header;
        System.out.println("header set to "+myHeader);
    }
}