package LZ77;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Komprimiert und dekomprimiert eine Datei mit dem LZ77-Verfahren.
 * @author Patrick Fritschi
 * @version 1.0
 */
public class Compression {
	
	BufferedReader in;
	DataOutputStream out;
	
	DataInputStream inStr;
	BufferedWriter outBuf;
	
	private static final int SUCHBUFFER_LENGHT = 10000;
	private static final int VORSCHAUBUFFER_LENGTH = 5000;
	
	StringBuffer suchBuffer = new StringBuffer(SUCHBUFFER_LENGHT);
	StringBuffer vorschauBuffer = new StringBuffer(VORSCHAUBUFFER_LENGTH);
	StringBuffer schreibBuffer = new StringBuffer(SUCHBUFFER_LENGHT+VORSCHAUBUFFER_LENGTH);
	
	public static void main(String[] args)
	{
		Compression compression = new Compression();
		compression.EncodeFile("C:\\Temp\\data_1.txt");
		System.out.println();
		compression.DecodeFile("C:\\Temp\\data_1.txt.lz77");
	}
	
	/**
	 * Dekodiert eine komprimierte Datei.
	 * @param filePath	die zu dekomprimierende Datei
	 */
	public void DecodeFile(String filePath)
	{
		long startTime = System.currentTimeMillis();
		long fileSize = new File(filePath).length();
		
		System.out.println("Decompression of " + filePath + " [" + (fileSize/1024) + " KB]");

		try
		{
			inStr = new DataInputStream( new FileInputStream( filePath));
			outBuf = new BufferedWriter(new FileWriter(filePath.replace(".lz77", "_decoded.txt")));		
			
			Boolean conti = true;
			while (conti)
			{
				BlockData encData = new BlockData();
				
				try
				{
					encData.offset = inStr.readShort();
					encData.charsIn = inStr.readShort();
					encData.nextChar = (char)inStr.readByte();
					
					String text = DecodeBlock(schreibBuffer,encData);
					AddTextToBuffer(schreibBuffer,text);
					
				}
				catch(EOFException ex)
				{
					conti = false;
					break;
				}
			}
			WriteBufferToFile(schreibBuffer, schreibBuffer.length());
			outBuf.close();
			System.out.println("File decompressed.");
		}
		catch(Exception ex)
		{
			//ex.getLocalizedMessage()
			System.out.println(ex.toString());
			System.out.println("Decompression failed");
		}
		
		long stopTime = System.currentTimeMillis();
	    long elapsedTime = stopTime - startTime;
	    System.out.println("Execution time: " + (elapsedTime/1000) + " sec");
		
	}
	
	/**
	 * Fügt den angegebenen Text zum Buffer hinzu.
	 * @param strBuf	der Schreib-Buffer
	 * @param text		der Text der zum Buffer hinzugefügt werden soll
	 */
	public void AddTextToBuffer(StringBuffer strBuf, String text)
	{
		if((strBuf.length() + text.length()) > SUCHBUFFER_LENGHT)
		{
			WriteBufferToFile(strBuf, (strBuf.length() + text.length()) - SUCHBUFFER_LENGHT);
		}
		
		strBuf.append(text);
		
	}
	
	/**
	 * Schreibt die Anzahl angegebenen Zeichen vom Anfang eines Buffers in eine Datei und
	 * entfernt diese aus dem Buffer.
	 * @param strBuf	der Buffer welcher geschrieben werden soll
	 * @param length	Anzahl Zeichen die geschrieben weden sollen
	 */
	public void WriteBufferToFile(StringBuffer strBuf, int length)
	{
		
		try
		{
			outBuf.write( strBuf.substring( 0,  length) );
			outBuf.flush();
			strBuf.delete( 0, length);
		}
		catch(Exception ex)
		{
			System.out.println("Could not write in to the target file.");
		}
	}
	
	/**
	 * Wandelt einen Block in Klartext um.
	 * @param strBuf	der Suchbuffer
	 * @param block		der aktuelle Block
	 * @return			Klartext des Blocks
	 */
	public String DecodeBlock(StringBuffer strBuf, BlockData block)
	{
		return strBuf.substring(strBuf.length() - block.offset, strBuf.length() - block.offset + block.charsIn).toString() + block.nextChar;
	}
	
	/**
	 * Komprimiert eine Datei mit LZ77.
	 * @param filePath		die zu komprimierende Datei
	 */
	public void EncodeFile(String filePath)
	{
		
		long startTime = System.currentTimeMillis();
		long fileSize = new File(filePath).length();
		
		System.out.println("Compression of " + filePath + " [" + (fileSize/1024) + " KB]");
		
		try { 
			in = new BufferedReader( new FileReader( filePath));
			out = new DataOutputStream(new FileOutputStream(filePath + ".lz77"));
		
			
			int c;
			int match = 0;
			int lastMatch = 0;
			while ((c = in.read()) != -1)   //   reads a single character from the input file
			{			
				vorschauBuffer.append((char)c);
				//System.out.print(suchBuffer.toString()+"|"+vorschauBuffer.toString() + " ");
				match = FindMatch(suchBuffer,vorschauBuffer);
				if(match > 0 && vorschauBuffer.length()<VORSCHAUBUFFER_LENGTH)
				{
					lastMatch = match;
				}
				else
				{
					WriteCompressedData(new BlockData(lastMatch,vorschauBuffer.length()-1,vorschauBuffer.charAt(vorschauBuffer.length()-1)),out);
					//System.out.print("[" + lastMatch + "," + (vorschauBuffer.length()-1) + "," + vorschauBuffer.charAt(vorschauBuffer.length()-1) + "]");
					CreateSpace(suchBuffer,vorschauBuffer.length());
					MoveVorschauToSuchBuffer(suchBuffer,vorschauBuffer);
				}
				//System.out.println();
			}
			out.flush();
			System.out.println("File compressed.");
			
			long stopTime = System.currentTimeMillis();
		    long elapsedTime = stopTime - startTime;
		    System.out.println("Execution time: " + (elapsedTime/1000) + " sec");
		}
		catch(Exception ex)
		{
			System.out.println(ex.toString());
		}		
	}
	
	/**
	 * Löscht Zeichen aus dem Buffer, falls dieser neue Text platz braucht.
	 * @param strBuf	der Buffer der benutzt werden soll.
	 * @param size		die Länge die in den Buffer eingefügt werde muss.
	 */
	public void CreateSpace(StringBuffer strBuf, int size)
	{
		if((strBuf.length()+size) > SUCHBUFFER_LENGHT)
		{
			strBuf = strBuf.delete(0,(strBuf.length() + size - SUCHBUFFER_LENGHT));
		}
	}
	
	/**
	 * Verschiebt den Vorschau-Buffer in den Such Buffer
	 * @param suchBuf		der Such-Buffer
	 * @param vorschauBuf	der Vorschau-Buffer
	 */
	public void MoveVorschauToSuchBuffer(StringBuffer suchBuf, StringBuffer vorschauBuf)
	{
		suchBuf.append(vorschauBuf);
		vorschauBuf.delete(0, vorschauBuf.length());
	}
	
	/**
	 * Sucht eine Vorkommen vom Vorschau-Buffer im Such-Buffer
	 * @param suchBuf	der Such-Buffer
	 * @param vorschauBuf	der Vorschau-Buffer
	 * @return
	 */
	private int FindMatch(StringBuffer suchBuf, StringBuffer vorschauBuf)
	{
		int offset = suchBuf.indexOf(vorschauBuf.toString());		
		
		if(offset >= 0)
		{
			return suchBuf.length()-offset;
		}
		else
		{
			return 0;
		}
	}
	
	/**
	 * Schreibt einen kodierten Block in eine Datei
	 * @param encData	der kodierte Block
	 * @param out		der Output-Stream
	 * @throws IOException	Falls der Block nicht geschrieben werden kann.
	 */
	private void WriteCompressedData( BlockData encData, DataOutputStream out) throws IOException {
		out.writeShort(encData.offset);
		out.writeShort(encData.charsIn);
		out.writeByte(encData.nextChar);
	}
	
}
