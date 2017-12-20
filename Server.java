

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Random;

public class Server {

	private String delimiter = "[.]";
	private DatagramSocket datagramSocket = null;
	private DatagramPacket datagramPacket = null;
	private byte[] receivedSegment = new byte[10000000];
	private String[] splittedSourceIpAddress;
	private String[] splittedDestinationIpAddress;
	private String protocolType = null;
	private String udpSegmentLength;
	private boolean isChecksumValid = false;
	private int expectedSequenceNumber = 0;
	private byte ackPacket[] = new byte[8];
	private int receivedSeqNum;
	private int receivedChecksum;
	private int allZeroesInAckPacket = 0;
	private int indicatorOfAckPacket = Integer.parseInt("1010101010101010",2);
	private byte[] segment;
	private String seqNumberInBinary;
	private int indicationOfDataSegment;
	FileOutputStream fileOutputStream = null;
	BufferedOutputStream bufferedOutputStream = null;
	int bytesRead;
	int currentBytesRead = 0;
	byte [] fileAsArray = new byte[60000000];
	private String[] sequenceNumber = new String[4];
	private boolean dropPacket = false;
	int myPortNumber;
	String fileName;
	double probability;
	
	public void receiveSegment() throws Exception {
		
		System.out.println("Server is up at port number "+myPortNumber);
		datagramSocket = new DatagramSocket(myPortNumber);
		
		while(true) {
		datagramPacket = new DatagramPacket(receivedSegment, receivedSegment.length);
		datagramSocket.receive(datagramPacket);
		
		
		double random = Math.random();
		if(random <= probability ) {
			dropPacket = true;
			System.out.println("Packet loss, sequence number = " +expectedSequenceNumber);
		}
		
		
		
		if(!dropPacket) {
		
		segment = Arrays.copyOf(datagramPacket.getData(), datagramPacket.getLength());
        
		readHeaderFromReceivedSegment();
		
		//Call the recalculateChecksum
        
		InetAddress sourceIPAddress = datagramPacket.getAddress();
		int portNumberOfReceivedPacket = datagramPacket.getPort();
		InetAddress serverIPAddress = InetAddress.getByName("localhost");
		
		//Check the sequence numbers
		
		
		if(expectedSequenceNumber == receivedSeqNum ) {
			if(reCalculateUDPChecksum(sourceIPAddress, serverIPAddress, seqNumberInBinary, indicationOfDataSegment, receivedChecksum, segment)) {
				createAckPacket();
				DatagramPacket datagramPacketForClient = new DatagramPacket(ackPacket, ackPacket.length, sourceIPAddress, portNumberOfReceivedPacket);
		        datagramSocket.send(datagramPacketForClient);
		        storeInFile();
		        expectedSequenceNumber++;
		        isChecksumValid = false;
			}
		}
		
		if(receivedSeqNum < expectedSequenceNumber || receivedSeqNum > expectedSequenceNumber) {
			createAckPacket();
			DatagramPacket datagramPacketForClient = new DatagramPacket(ackPacket, ackPacket.length, sourceIPAddress, portNumberOfReceivedPacket);
	        datagramSocket.send(datagramPacketForClient);
		}
		if(indicationOfDataSegment == 0)
			break;
		}
		dropPacket = false;
		}
	}
	
	public void readHeaderFromReceivedSegment() {
		sequenceNumber[0] = String.format("%"+Integer.toString(8)+"s",Integer.toBinaryString(segment[0] & 0xFF)).replace(" ","0");
		sequenceNumber[1] = String.format("%"+Integer.toString(8)+"s",Integer.toBinaryString(segment[1] & 0xFF)).replace(" ","0");
		sequenceNumber[2] = String.format("%"+Integer.toString(8)+"s",Integer.toBinaryString(segment[2] & 0xFF)).replace(" ","0");
		sequenceNumber[3] = String.format("%"+Integer.toString(8)+"s",Integer.toBinaryString(segment[3] & 0xFF)).replace(" ","0");
		seqNumberInBinary = sequenceNumber[0] + sequenceNumber[1] + sequenceNumber[2] + sequenceNumber[3];
		receivedSeqNum = Integer.parseInt(seqNumberInBinary, 2);
		
		String checksumTest1 = String.format("%"+Integer.toString(8)+"s",Integer.toBinaryString(segment[4] & 0xFF)).replace(" ","0");
		String checksumTest2 = String.format("%"+Integer.toString(8)+"s",Integer.toBinaryString(segment[5] & 0xFF)).replace(" ","0");
		receivedChecksum = Integer.parseInt(checksumTest1 + checksumTest2,2);
		
		String indicationOfDataSegmentTest1 = String.format("%"+Integer.toString(8)+"s",Integer.toBinaryString(segment[6] & 0xFF)).replace(" ","0");
		String indicationOfDataSegmentTest2 = String.format("%"+Integer.toString(8)+"s",Integer.toBinaryString(segment[7] & 0xFF)).replace(" ","0");
		indicationOfDataSegment = Integer.parseInt(indicationOfDataSegmentTest1 + indicationOfDataSegmentTest2,2);
	}
	
	public boolean reCalculateUDPChecksum(InetAddress sourceIPAddressOfSegment, InetAddress destinationIPAddressOfSegment, String sequenceNumberInBinary, int indicationOfDataSegment, int checksumOfSegment, byte[] segment) throws UnknownHostException {
		String firstByteInBinary;
		String secondByteInBinary;
		String thirdByteInBinary;
		String fourthByteInBinary;
		String[] sourceIpAs16BitString = new String[2]; 
		String[] destinationIpAs16BitString = new String[2];
		String[] sequenceNumberInBinaryAs16BitString = new String[2];
		String[] dataInSegmentInBinaryAs8BitString = new String[2];
		int sum = 0;
		String checksum;
		int bytesTraversed;
		/*
		 * In order to get 16 bit binary strings for checksum calculation:
		 * 1. Splitting the IP address using '.' as the delimiter since IP address is in that form. 
		 * 2. Converting each byte of IP address into binary string of 8 bits.
		 * 3. Combining (1st, 2nd) and (3rd, 4th) binary strings to make it a binary string of 16 bits.
		 * 4. Adding these 16 bit strings to the sum.
		 * */
		
		splittedSourceIpAddress = (sourceIPAddressOfSegment.getHostAddress()).split(delimiter);
		firstByteInBinary = String.format("%"+Integer.toString(8)+"s",Integer.toBinaryString(Integer.parseInt(splittedSourceIpAddress[0]) & 0xFF)).replace(" ","0");
		secondByteInBinary = String.format("%"+Integer.toString(8)+"s",Integer.toBinaryString(Integer.parseInt(splittedSourceIpAddress[1]) & 0xFF)).replace(" ","0");
		thirdByteInBinary = String.format("%"+Integer.toString(8)+"s",Integer.toBinaryString(Integer.parseInt(splittedSourceIpAddress[2]) & 0xFF)).replace(" ","0");
		fourthByteInBinary = String.format("%"+Integer.toString(8)+"s",Integer.toBinaryString(Integer.parseInt(splittedSourceIpAddress[3]) & 0xFF)).replace(" ","0");
		sourceIpAs16BitString[0] = firstByteInBinary + secondByteInBinary;
		sourceIpAs16BitString[1] = thirdByteInBinary + fourthByteInBinary;
		//sum += Integer.parseInt(sourceIpAs16BitString[0],2) + Integer.parseInt(sourceIpAs16BitString[1],2);
		
		splittedDestinationIpAddress = (destinationIPAddressOfSegment.getHostAddress()).split(delimiter);
		firstByteInBinary = String.format("%"+Integer.toString(8)+"s",Integer.toBinaryString(Integer.parseInt(splittedDestinationIpAddress[0]) & 0xFF)).replace(" ","0");
		secondByteInBinary = String.format("%"+Integer.toString(8)+"s",Integer.toBinaryString(Integer.parseInt(splittedDestinationIpAddress[1]) & 0xFF)).replace(" ","0");
		thirdByteInBinary = String.format("%"+Integer.toString(8)+"s",Integer.toBinaryString(Integer.parseInt(splittedDestinationIpAddress[2]) & 0xFF)).replace(" ","0");
		fourthByteInBinary = String.format("%"+Integer.toString(8)+"s",Integer.toBinaryString(Integer.parseInt(splittedDestinationIpAddress[3]) &0xFF)).replace(" ","0");
		destinationIpAs16BitString[0] = firstByteInBinary + secondByteInBinary;
		destinationIpAs16BitString[1] = thirdByteInBinary + fourthByteInBinary;
		//sum += Integer.parseInt(destinationIpAs16BitString[0],2) + Integer.parseInt(destinationIpAs16BitString[1],2);
		/*
		 * protocol type number '17' represents UDP. Hence, it is used explicitly.
		 * Further, converting it to a 16 bit binary string.
		 * */
		protocolType = String.format("%"+Integer.toString(16)+"s",Integer.toBinaryString((17)  & 0xFFFF)).replace(" ","0");
		sum += Integer.parseInt(protocolType,2);
		//System.out.println("Value of protocol  "+protocolType);
		//System.out.println("sum 1 " +sum);
		/*
		 * The UDP segment length in bytes would be (maximumSegmentSize + 6) because 
		 * data in each segment would contain MSS number of bytes and additional 
		 * 6 bytes (4 bytes of UDP sequence number + 2 bytes of indication that this is a data packet).
		 * It does not include 2 bytes of UDP checksum because that is what is calculated here and 
		 * would then be attached and hence the UDP segment length should not comprise of UDP checksum length. 
		 */
		udpSegmentLength = String.format("%"+Integer.toString(16)+"s",Integer.toBinaryString((segment.length - 2) & 0xFFFF)).replace(" ","0");
		sum += Integer.parseInt(udpSegmentLength,2);
		
		
		/*
		 * Splitting the 32 bit sequence number into two strings of 16 bits each and then adding them to the sum.
		 * */
		sequenceNumberInBinaryAs16BitString[0] = sequenceNumberInBinary.substring(0, 16);
		sequenceNumberInBinaryAs16BitString[1] = sequenceNumberInBinary.substring(16, 32);
		sum += Integer.parseInt(sequenceNumberInBinaryAs16BitString[0],2) + Integer.parseInt(sequenceNumberInBinaryAs16BitString[1],2);
		
		sum += indicationOfDataSegment;
		
		
		/*
		 * 1. Checking whether MSS is a multiple of 2 or not. If not then the last byte 
		 *    would be an individual byte and one byte of zeros (00000000) will be put 
		 *    beyond the last byte to make it a 16 bit binary string.
		 * 2. Converting each byte(8 bits) in byte array to binary string.
		 * 3. Merging these two strings to form a 16 bit binary string.
		 * 4. Taking the integer value of the 16 bit binary in the string and adding it to the sum.
		 * 5. Continuing this till the MSS is not reached. 
		 * 6. 'i' incremented by 2 in each cycle because operations on 2 bytes (16 bits) takes place.
		 * */
		if((segment.length-8) % 2 == 0) {
			for(bytesTraversed = 8; bytesTraversed < segment.length; bytesTraversed += 2) {
				dataInSegmentInBinaryAs8BitString[0] = String.format("%"+Integer.toString(8)+"s",Integer.toBinaryString(segment[bytesTraversed] & 0xFF)).replace(" ","0");
				dataInSegmentInBinaryAs8BitString[1] = String.format("%"+Integer.toString(8)+"s",Integer.toBinaryString(segment[bytesTraversed + 1] & 0xFF)).replace(" ","0");
				sum += Integer.parseInt((dataInSegmentInBinaryAs8BitString[0] + dataInSegmentInBinaryAs8BitString[1]),2);
			}
		}
		else {
			for(bytesTraversed = 8; bytesTraversed < segment.length - 1; bytesTraversed += 2) {
				dataInSegmentInBinaryAs8BitString[0] = String.format("%"+Integer.toString(8)+"s",Integer.toBinaryString(segment[bytesTraversed] & 0xFF)).replace(" ","0");
				dataInSegmentInBinaryAs8BitString[1] = String.format("%"+Integer.toString(8)+"s",Integer.toBinaryString(segment[bytesTraversed+1] & 0xFF)).replace(" ","0");
				sum += Integer.parseInt((dataInSegmentInBinaryAs8BitString[0] + dataInSegmentInBinaryAs8BitString[1]),2);
			}
			dataInSegmentInBinaryAs8BitString[0] = String.format("%"+Integer.toString(8)+"s",Integer.toBinaryString(segment[bytesTraversed] & 0xFF)).replace(" ","0");
			dataInSegmentInBinaryAs8BitString[1] = "00000000";
			sum += Integer.parseInt((dataInSegmentInBinaryAs8BitString[0] + dataInSegmentInBinaryAs8BitString[1]),2);
		}
		
		/*
		 * If overflow is present then it is added to the lower 16 bits
		 * and this continues till the time there is no overflow.
		 * */
		while(sum > 65535) {
			String sumAsHexString;
			String[] hexString = new String[2];
			String overflowAs16BitBinaryString;
			String lowerHalfAs16BitBinaryString;
			sumAsHexString = Integer.toHexString(sum);
			hexString[0] = sumAsHexString.substring(0, sumAsHexString.length()-4);
			hexString[1] = sumAsHexString.substring(sumAsHexString.length()-4, sumAsHexString.length());
			overflowAs16BitBinaryString = String.format("%"+Integer.toString(16)+"s",Integer.toBinaryString(Integer.parseInt(hexString[0], 16) & 0xFFFF)).replace(" ","0");
			lowerHalfAs16BitBinaryString = new BigInteger(hexString[1],16).toString(2);
			sum = Integer.parseInt(overflowAs16BitBinaryString, 2) + Integer.parseInt(lowerHalfAs16BitBinaryString, 2);
		}
		
		int sumOfCalculatedAndReceivedChecksum = sum + checksumOfSegment;
		
		if(sumOfCalculatedAndReceivedChecksum == 65535) {
			isChecksumValid = true;
		}
		return isChecksumValid;
	}
	
	public void createAckPacket() {
		if(expectedSequenceNumber == receivedSeqNum ) {
			ackPacket[0] = (byte) ((receivedSeqNum >> 24) & 0xFF);
			ackPacket[1] = (byte) ((receivedSeqNum >> 16) & 0xFF);
			ackPacket[2] = (byte) ((receivedSeqNum >> 8) & 0xFF);
			ackPacket[3] = (byte) (receivedSeqNum & 0xFF);
			
		}
		
		if(receivedSeqNum < expectedSequenceNumber || receivedSeqNum > expectedSequenceNumber) {
			ackPacket[0] = (byte) (((expectedSequenceNumber-1) >> 24) & 0xFF);
			ackPacket[1] = (byte) (((expectedSequenceNumber-1) >> 16) & 0xFF);
			ackPacket[2] = (byte) (((expectedSequenceNumber-1) >> 8) & 0xFF);
			ackPacket[3] = (byte) ((expectedSequenceNumber-1) & 0xFF);
			
		}
		
		ackPacket[4] = (byte) ((allZeroesInAckPacket >>> 8) & 0xFF);
		ackPacket[5] = (byte) (allZeroesInAckPacket & 0xFF);
		
		ackPacket[6]= (byte) ((indicatorOfAckPacket >>> 8) & 0xFF);
		ackPacket[7]= (byte) (indicatorOfAckPacket & 0xFF);
	}
	
	public void storeInFile() throws Exception{
		fileOutputStream = new FileOutputStream(System.getProperty("user.dir") + "/"+fileName+".txt",true);
		bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
		bufferedOutputStream.write(segment, 8, segment.length-8);
		bufferedOutputStream.flush();	
		bufferedOutputStream.close();
		fileOutputStream.close();
		
	}
	
	public static void main(String[] args) throws Exception {
		Server server = new Server();
		server.myPortNumber = Integer.parseInt(args[1]);
		server.fileName = args[2];
		server.probability = Double.parseDouble(args[3]);
		server.receiveSegment();
		
	}

}
