
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.bind.DatatypeConverterInterface;

import org.omg.CosNaming.IstringHelper;

public class Client {

	private int maximumSegmentSize;
	private byte[] dataInSegment;
	private byte[] lastDataInSegment;
	private byte[] segment;
	private byte[] acknowledgement = new byte[8];
	private File file;
	private int numberOfBytesReceived = 1;
	private boolean isEndOfFile = false;
	private DatagramSocket datagramSocketForServer1 = null;
	private DatagramSocket datagramSocketForServer2 = null;
	private DatagramSocket datagramSocketForServer3 = null;
	private DatagramSocket datagramSocketForServer4 = null;
	private DatagramSocket datagramSocketForServer5 = null;
	private DatagramPacket datagramPacketForServer1 = null;
	private DatagramPacket datagramPacketForServer2 = null;
	private DatagramPacket datagramPacketForServer3 = null;
	private DatagramPacket datagramPacketForServer4 = null;
	private DatagramPacket datagramPacketForServer5 = null;
	private DatagramPacket datagramPacketFromServer1 = null;
	private DatagramPacket datagramPacketFromServer2 = null;
	private DatagramPacket datagramPacketFromServer3 = null;
	private DatagramPacket datagramPacketFromServer4 = null;
	private DatagramPacket datagramPacketFromServer5 = null;
	private InetAddress iNetAddressOfServer1 = null;
	private InetAddress iNetAddressOfServer2 = null;
	private InetAddress iNetAddressOfServer3 = null;
	private InetAddress iNetAddressOfServer4 = null;
	private InetAddress iNetAddressOfServer5 = null;
	private int numberOfServers;
	private int portNumberOfServer;
	private int sequenceNumberInDecimal = 0;
	private byte[] sequenceNumber = new byte[4];
	private String sequenceNumberInBinary;
	private int lengthOfSequenceNumber;
	private String sourceIPAddress = null;
	private String destinationIPAddress = null;
	private String protocolType = null;
	private String udpSegmentLength;
	private String[] splittedSourceIpAddress;
	private String[] splittedDestinationIpAddress;
	private String delimiter = "[.]";
	private int indicationOfDataSegment = Integer.parseInt("0101010101010101",2);
	private byte[] indicationOfDataSegmentByteArray = new byte[2];
	private int indicationOfLastDataSegment = Integer.parseInt("0000000000000000", 2);
	/*
	 * see if this can be done with a single checksum. I think can be done because as it is I am sending
	 * segment to each server one after the other. It might become a problem if I do multithreading. 
	 * Also another problem with multithreading would be communication between them. This is because
	 * next segment has to be sent only after ACK has been received from all the servers. So each one will
	 * have to signal all the threads after reception of ACK and implementing this logic can be tedious.
	 * */
	private int checksumForServer1;
	private byte[] checksumForServer1ByteArray = new byte[2];
	private int checksumForServer2;
	private byte[] checksumForServer2ByteArray = new byte[2];
	private int checksumForServer3;
	private byte[] checksumForServer3ByteArray = new byte[2];
	private int checksumForServer4;
	private byte[] checksumForServer4ByteArray = new byte[2];
	private int checksumForServer5;
	private byte[] checksumForServer5ByteArray = new byte[2];
	private String[] receivedSequenceNumber = new String[4];
	private int receivedSequenceNumberInDecimal;
	private String[] receivedFieldOfZeros = new String[2];
	private int receivedFieldOfZerosInDecimal;
	private String[] indicationOfAcknowledgementPacket = new String[2];
	private int indicationOfAcknowledgementPacketInDecimal;
	private boolean isGoodToSendNextSegment = false;
	private Map <String, Integer> map = new HashMap<String,Integer>();
	private double startTime;
	private double endTime;
	private double timeToTransferFile = 0;
	private static ArrayList<String> clientInputArrayList = new ArrayList<String>();
	private Thread threadForServer1;
	private Thread threadForServer2;
	private Thread threadForServer3;
	private Thread threadForServer4;
	private Thread threadForServer5;
	
	/*
	 * If timeout has occured, it means that there was a problem with ACK from 
	 * atleast one of the servers. Depending on which server is not in the 
	 * position to receive the next segment, retransmission is done by the
	 * client to that particular server only. Once all necessary retransmissions
	 * are done, it needs to wait for ACK to these retransmissions and this is done
	 * by calling handleAcknowledgements(). The handleAcknowledgements() method
	 * has such a logic that it will wait for ACKs only from those servers whom 
	 * the retransmission was done.  
	 * */
	private class TimeoutHandlingTask extends TimerTask{

		@Override
		public void run() {
			System.out.println("Timeout, Sequence number = " +sequenceNumberInDecimal);
			try {
				switch(numberOfServers) {
				case 1: 
					if(map.get(iNetAddressOfServer1.getHostAddress()) == 0)
						sendSegmentToServer1();
					break;
				case 2:
					if(map.get(iNetAddressOfServer1.getHostAddress()) == 0) {
						sendSegmentToServer1();
					}
					if(map.get(iNetAddressOfServer2.getHostAddress()) == 0) {
						sendSegmentToServer2();
					}
					break;
				case 3:
					if(map.get(iNetAddressOfServer1.getHostAddress()) == 0)
						sendSegmentToServer1();
					if(map.get(iNetAddressOfServer2.getHostAddress()) == 0)
						sendSegmentToServer2();
					if(map.get(iNetAddressOfServer3.getHostAddress()) == 0)
						sendSegmentToServer3();
					break;
				case 4:
					if(map.get(iNetAddressOfServer1.getHostAddress()) == 0)
						sendSegmentToServer1();
					if(map.get(iNetAddressOfServer2.getHostAddress()) == 0)
						sendSegmentToServer2();
					if(map.get(iNetAddressOfServer3.getHostAddress()) == 0)
						sendSegmentToServer3();
					if(map.get(iNetAddressOfServer4.getHostAddress()) == 0)
						sendSegmentToServer4();
					break;
				case 5:
					if(map.get(iNetAddressOfServer1.getHostAddress()) == 0)
						sendSegmentToServer1();
					if(map.get(iNetAddressOfServer2.getHostAddress()) == 0)
						sendSegmentToServer2();
					if(map.get(iNetAddressOfServer3.getHostAddress()) == 0)
						sendSegmentToServer3();
					if(map.get(iNetAddressOfServer4.getHostAddress()) == 0)
						sendSegmentToServer4();
					if(map.get(iNetAddressOfServer5.getHostAddress()) == 0)
						sendSegmentToServer5();
					break;
				default:
					break;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}	
		}
	}
	
	Runnable threadToReceiveAcknowledgementsFromServer1 = new Runnable() {
		
		@Override
		public void run() {
			while(true) {
				datagramPacketFromServer1 = new DatagramPacket(acknowledgement, acknowledgement.length);
				try {
					datagramSocketForServer1.receive(datagramPacketFromServer1);
					readAndProcessAcknowledgement(datagramPacketFromServer1.getAddress().getHostAddress());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	};
	
	Runnable threadToReceiveAcknowledgementsFromServer2 = new Runnable() {
		
		@Override
		public void run() {
			while(true) {
				datagramPacketFromServer2 = new DatagramPacket(acknowledgement, acknowledgement.length);
				try {
					datagramSocketForServer2.receive(datagramPacketFromServer2);
					readAndProcessAcknowledgement(datagramPacketFromServer2.getAddress().getHostAddress());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	};
	
	Runnable threadToReceiveAcknowledgementsFromServer3 = new Runnable() {
		
		@Override
		public void run() {
			while(true) {
				datagramPacketFromServer3 = new DatagramPacket(acknowledgement, acknowledgement.length);
				try {
					datagramSocketForServer3.receive(datagramPacketFromServer3);
					readAndProcessAcknowledgement(datagramPacketFromServer3.getAddress().getHostAddress());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	};
	
	Runnable threadToReceiveAcknowledgementsFromServer4 = new Runnable() {
		
		@Override
		public void run() {
			while(true) {
				datagramPacketFromServer4 = new DatagramPacket(acknowledgement, acknowledgement.length);
				try {
					datagramSocketForServer4.receive(datagramPacketFromServer4);
					readAndProcessAcknowledgement(datagramPacketFromServer4.getAddress().getHostAddress());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	};
	
	Runnable threadToReceiveAcknowledgementsFromServer5 = new Runnable() {
		
		@Override
		public void run() {
			while(true) {
				datagramPacketFromServer5 = new DatagramPacket(acknowledgement, acknowledgement.length);
				try {
					datagramSocketForServer5.receive(datagramPacketFromServer5);
					readAndProcessAcknowledgement(datagramPacketFromServer5.getAddress().getHostAddress());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	};
	
	/*
	 * Constructor. Takes input as server names, filename and maxiumum segment size(MSS)
	 * */
	public Client(String... args) throws IOException, InterruptedException {
		switch (clientInputArrayList.size()) {
		case 5:
			this.numberOfServers = 1;
			file = new File(args[2]);
			this.maximumSegmentSize = Integer.parseInt(args[3]);
			this.datagramSocketForServer1 = new DatagramSocket();
			this.iNetAddressOfServer1 = InetAddress.getByName(args[0]);
			this.portNumberOfServer = Integer.parseInt(args[1]);
			map.put(this.iNetAddressOfServer1.getHostAddress(), 0);
			this.threadForServer1 = new Thread(threadToReceiveAcknowledgementsFromServer1);
			this.threadForServer1.start();
			break;
		case 6:
			this.numberOfServers = 2;
			file = new File(args[3]);
			this.maximumSegmentSize = Integer.parseInt(args[4]);
			this.datagramSocketForServer1 = new DatagramSocket();
			this.datagramSocketForServer2 = new DatagramSocket();
			this.iNetAddressOfServer1 = (InetAddress.getByName(args[0]));
			this.iNetAddressOfServer2 = (InetAddress.getByName(args[1]));
			this.portNumberOfServer = Integer.parseInt(args[2]);
			map.put(this.iNetAddressOfServer1.getHostAddress(), 0);
			map.put(this.iNetAddressOfServer2.getHostAddress(), 0);
			this.threadForServer1 = new Thread(threadToReceiveAcknowledgementsFromServer1);
			this.threadForServer1.start();
			this.threadForServer2 = new Thread(threadToReceiveAcknowledgementsFromServer2);
			this.threadForServer2.start();
			break;
		case 7:
			this.numberOfServers = 3;
			file = new File(args[4]);
			this.maximumSegmentSize = Integer.parseInt(args[5]);
			this.datagramSocketForServer1 = new DatagramSocket();
			this.datagramSocketForServer2 = new DatagramSocket();
			this.datagramSocketForServer3 = new DatagramSocket();
			this.iNetAddressOfServer1 = (InetAddress.getByName(args[0]));
			this.iNetAddressOfServer2 = (InetAddress.getByName(args[1]));
			this.iNetAddressOfServer3 = (InetAddress.getByName(args[2]));
			this.portNumberOfServer = Integer.parseInt(args[3]);
			map.put(this.iNetAddressOfServer1.getHostAddress(), 0);
			map.put(this.iNetAddressOfServer2.getHostAddress(), 0);
			map.put(this.iNetAddressOfServer3.getHostAddress(), 0);
			this.threadForServer1 = new Thread(threadToReceiveAcknowledgementsFromServer1);
			this.threadForServer1.start();
			this.threadForServer2 = new Thread(threadToReceiveAcknowledgementsFromServer2);
			this.threadForServer2.start();
			this.threadForServer3 = new Thread(threadToReceiveAcknowledgementsFromServer3);
			this.threadForServer3.start();
			break;
		case 8: 
			this.numberOfServers = 4;
			file = new File(args[5]);
			this.maximumSegmentSize = Integer.parseInt(args[6]);
			this.datagramSocketForServer1 = new DatagramSocket();
			this.datagramSocketForServer2 = new DatagramSocket();
			this.datagramSocketForServer3 = new DatagramSocket();
			this.datagramSocketForServer4 = new DatagramSocket();
			this.iNetAddressOfServer1 = (InetAddress.getByName(args[0]));
			this.iNetAddressOfServer2 = (InetAddress.getByName(args[1]));
			this.iNetAddressOfServer3 = (InetAddress.getByName(args[2]));
			this.iNetAddressOfServer4 = (InetAddress.getByName(args[3]));
			this.portNumberOfServer = Integer.parseInt(args[4]);
			map.put(this.iNetAddressOfServer1.getHostAddress(), 0);
			map.put(this.iNetAddressOfServer2.getHostAddress(), 0);
			map.put(this.iNetAddressOfServer3.getHostAddress(), 0);
			map.put(this.iNetAddressOfServer4.getHostAddress(), 0);
			this.threadForServer1 = new Thread(threadToReceiveAcknowledgementsFromServer1);
			this.threadForServer1.start();
			this.threadForServer2 = new Thread(threadToReceiveAcknowledgementsFromServer2);
			this.threadForServer2.start();
			this.threadForServer3 = new Thread(threadToReceiveAcknowledgementsFromServer3);
			this.threadForServer3.start();
			this.threadForServer4 = new Thread(threadToReceiveAcknowledgementsFromServer4);
			this.threadForServer4.start();
			break;
		case 9: 
			this.numberOfServers = 5;
			file = new File(args[6]);
			this.maximumSegmentSize = Integer.parseInt(args[7]);
			this.datagramSocketForServer1 = new DatagramSocket();
			this.datagramSocketForServer2 = new DatagramSocket();
			this.datagramSocketForServer3 = new DatagramSocket();
			this.datagramSocketForServer4 = new DatagramSocket();
			this.datagramSocketForServer5 = new DatagramSocket();
			this.iNetAddressOfServer1 = (InetAddress.getByName(args[0]));
			this.iNetAddressOfServer2 = (InetAddress.getByName(args[1]));
			this.iNetAddressOfServer3 = (InetAddress.getByName(args[2]));
			this.iNetAddressOfServer4 = (InetAddress.getByName(args[3]));
			this.iNetAddressOfServer5 = (InetAddress.getByName(args[4]));
			this.portNumberOfServer = Integer.parseInt(args[5]);
			map.put(this.iNetAddressOfServer1.getHostAddress(), 0);
			map.put(this.iNetAddressOfServer2.getHostAddress(), 0);
			map.put(this.iNetAddressOfServer3.getHostAddress(), 0);
			map.put(this.iNetAddressOfServer4.getHostAddress(), 0);
			map.put(this.iNetAddressOfServer5.getHostAddress(), 0);
			this.threadForServer1 = new Thread(threadToReceiveAcknowledgementsFromServer1);
			this.threadForServer1.start();
			this.threadForServer2 = new Thread(threadToReceiveAcknowledgementsFromServer2);
			this.threadForServer2.start();
			this.threadForServer3 = new Thread(threadToReceiveAcknowledgementsFromServer3);
			this.threadForServer3.start();
			this.threadForServer4 = new Thread(threadToReceiveAcknowledgementsFromServer4);
			this.threadForServer4.start();
			this.threadForServer5 = new Thread(threadToReceiveAcknowledgementsFromServer5);
			this.threadForServer5.start();
			break;
		default:
			break;
		}
		this.dataInSegment = new byte[this.maximumSegmentSize];
		rdt_send();
	}

	/*
	 * File object is initialized in the constructor and is used by FileInputStream over here directly. 
	 * BufferedInputStream reads the file one byte at a time and passes this read byte to the stopAndWaitProtocol().
	 * */
	@SuppressWarnings("deprecation")
	private void rdt_send() throws IOException, InterruptedException {
		byte [] oneByteArray = new byte[1];
		FileInputStream fileInputStream = new FileInputStream(file);
		BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
		startTime = System.nanoTime();
		while(bufferedInputStream.read(oneByteArray) != -1) {
			stopAndWaitProtocol(oneByteArray);
		}
		isEndOfFile = true;
		stopAndWaitProtocol(oneByteArray);
		endTime = System.nanoTime();
		timeToTransferFile = (endTime - startTime)/1000000000.0;
		FileWriter fileWriter = new FileWriter("TimeToTransferFile");
		BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
		bufferedWriter.write(String.valueOf(timeToTransferFile));
		bufferedWriter.close();
	}
	
	/*	On receiving one byte from rdt_send(), the stop and wait protocol starts buffering it
	 *  in the dataInSegment array until MSS or end of file is reached. Once, one of the 
	 *  aforementioned conditions is satisfied, headers are attached to the data and this 
	 *  entire segment is sent over a UDP socket connection to the servers. The client 
	 *  goes into the wait state waiting for the acknowledgement from all the servers.
	 *  Once ACK is received from all servers, only then the client sends the next segment
	 *  to all the servers. A timeout counter is started while waiting for the ACK. 
	 *  If timeout occurs then retransmission to that particular server takes place. 
	 * */
	private void stopAndWaitProtocol(byte[] oneByteArray) throws IOException, InterruptedException {
		if(!isEndOfFile)
			dataInSegment[numberOfBytesReceived-1] = oneByteArray[0];
		if(numberOfBytesReceived == maximumSegmentSize || isEndOfFile) {
			if(!isEndOfFile)
				segment = new byte[maximumSegmentSize + 8];
			/*
			 * 1. (numberOfBytesReceived - 1) because no. of bytes had increased by 1 when stopAndWait() was called
			 * 	  when last byte was read by rdt_send(). 
			 * 2. Post that EOF flag is set and call to stopAndWait is made again. 
			 * 	  It is at this point the else if condition would be satisfied 
			 *    and the segment array of only actual number of bytes received is made.
			 * */
			else if(isEndOfFile) {
				numberOfBytesReceived -= 1;
				segment = new byte[numberOfBytesReceived + 8];
			} 
			sequenceNumberInBinary = String.format("%"+Integer.toString(32)+"s",Integer.toBinaryString(sequenceNumberInDecimal & 0xFFFFFFFF)).replace(" ","0");
			/*
			 * 1. Putting sequence number, checksum (different for each segment 
			 *    since involves dest. IP and hence handled in sendSegmentToServer() methods) 
			 * 	  and indication of data segment into byte arrays with 8 bits at each index 
			 *    of the byte array. This will prevent each bit getting stored as a 
			 *    byte (which happens if I just do .getBytes()).
			 * 2. Copying each byte array in the segment byte array thereby forming the entire segment.  
			 * */
			
			sequenceNumber[0] = (byte) ((sequenceNumberInDecimal >> 24) & 0xFF);
			sequenceNumber[1] = (byte) ((sequenceNumberInDecimal >> 16) & 0xFF);
			sequenceNumber[2] = (byte) ((sequenceNumberInDecimal >> 8) & 0xFF);
			sequenceNumber[3] = (byte) (sequenceNumberInDecimal & 0xFF);
			
			if(!isEndOfFile) {
				indicationOfDataSegmentByteArray[0] = (byte) ((indicationOfDataSegment >>> 8) & 0xFF);
				indicationOfDataSegmentByteArray[1] = (byte) (indicationOfDataSegment & 0xFF);
			}
			else if(isEndOfFile) {
				indicationOfDataSegmentByteArray[0] = (byte) ((indicationOfLastDataSegment >>> 8) & 0xFF);
				indicationOfDataSegmentByteArray[1] = (byte) (indicationOfLastDataSegment & 0xFF);
			}
			
			if(numberOfServers == 1) {
				sendSegmentToServer1();
				handleAcknowledgements();
			}
			else if(numberOfServers == 2) {
				sendSegmentToServer1();
				sendSegmentToServer2();
				handleAcknowledgements();
			}
			else if(numberOfServers == 3) {
				sendSegmentToServer1();
				sendSegmentToServer2();
				sendSegmentToServer3();
				handleAcknowledgements();
			}
			else if(numberOfServers == 4) {
				sendSegmentToServer1();
				sendSegmentToServer2();
				sendSegmentToServer3();
				sendSegmentToServer4();
				handleAcknowledgements();
			}
			else if(numberOfServers == 5) {
				sendSegmentToServer1();
				sendSegmentToServer2();
				sendSegmentToServer3();
				sendSegmentToServer4();
				sendSegmentToServer5();
				handleAcknowledgements();
			}
			numberOfBytesReceived = 1;
			for(int i=0; i < dataInSegment.length; i++) {
				dataInSegment[i] = 0;
			}
			for(int i=0; i < segment.length; i++) {
				segment[i] = 0;
			}
		}
		else numberOfBytesReceived++;
		isEndOfFile = false;
	}
	
	public void sendSegmentToServer1() throws IOException {
			checksumForServer1 = calculateUDPChecksum(iNetAddressOfServer1, sequenceNumberInBinary, indicationOfDataSegment, dataInSegment);
			
			checksumForServer1ByteArray[0] = (byte) ((checksumForServer1 >>> 8) & 0xFF);
			checksumForServer1ByteArray[1] = (byte) (checksumForServer1 & 0xFF);
			
			System.arraycopy(sequenceNumber, 0, segment, 0, sequenceNumber.length);
			System.arraycopy(checksumForServer1ByteArray, 0, segment, sequenceNumber.length, checksumForServer1ByteArray.length);
			System.arraycopy(indicationOfDataSegmentByteArray, 0, segment, (sequenceNumber.length + checksumForServer1ByteArray.length), indicationOfDataSegmentByteArray.length);
			/*
			 * The below check is done because dataInSegment's length is equal to MSS.
			 * But when the file has ended and data is less than MSS then the size of segment
			 * array depends on the number of bytes received. So segment should copy only
			 * those many no. of bytes else it will be out of bounds.
			 * */
			if(!isEndOfFile)
				System.arraycopy(dataInSegment, 0, segment, (sequenceNumber.length + checksumForServer1ByteArray.length + indicationOfDataSegmentByteArray.length), numberOfBytesReceived);
			else if(isEndOfFile)
				System.arraycopy(dataInSegment, 0, segment, (sequenceNumber.length + checksumForServer1ByteArray.length + indicationOfDataSegmentByteArray.length), numberOfBytesReceived);
		
			datagramPacketForServer1 = new DatagramPacket(segment, segment.length, iNetAddressOfServer1, portNumberOfServer);
			datagramSocketForServer1.send(datagramPacketForServer1);
	}
	
	public void sendSegmentToServer2() throws IOException {
			checksumForServer2 = calculateUDPChecksum(iNetAddressOfServer2, sequenceNumberInBinary, indicationOfDataSegment, dataInSegment);
			
			checksumForServer2ByteArray[0] = (byte) ((checksumForServer2 >>> 8) & 0xFF);
			checksumForServer2ByteArray[1] = (byte) (checksumForServer2 & 0xFF);
			
			System.arraycopy(sequenceNumber, 0, segment, 0, sequenceNumber.length);
			System.arraycopy(checksumForServer2ByteArray, 0, segment, sequenceNumber.length, checksumForServer2ByteArray.length);
			System.arraycopy(indicationOfDataSegmentByteArray, 0, segment, (sequenceNumber.length + checksumForServer2ByteArray.length), indicationOfDataSegmentByteArray.length);
			if(!isEndOfFile)
				System.arraycopy(dataInSegment, 0, segment, (sequenceNumber.length + checksumForServer2ByteArray.length + indicationOfDataSegmentByteArray.length), numberOfBytesReceived);
			else if(isEndOfFile)
				System.arraycopy(dataInSegment, 0, segment, (sequenceNumber.length + checksumForServer2ByteArray.length + indicationOfDataSegmentByteArray.length), numberOfBytesReceived);
				
			datagramPacketForServer2 = new DatagramPacket(segment, segment.length, iNetAddressOfServer2, portNumberOfServer);
			datagramSocketForServer2.send(datagramPacketForServer2);
	}
	
	public void sendSegmentToServer3() throws IOException {
		checksumForServer3 = calculateUDPChecksum(iNetAddressOfServer3, sequenceNumberInBinary, indicationOfDataSegment, dataInSegment);
		
		checksumForServer3ByteArray[0] = (byte) ((checksumForServer3 >>> 8) & 0xFF);
		checksumForServer3ByteArray[1] = (byte) (checksumForServer3 & 0xFF);
		
		System.arraycopy(sequenceNumber, 0, segment, 0, sequenceNumber.length);
		System.arraycopy(checksumForServer3ByteArray, 0, segment, sequenceNumber.length, checksumForServer3ByteArray.length);
		System.arraycopy(indicationOfDataSegmentByteArray, 0, segment, (sequenceNumber.length + checksumForServer3ByteArray.length), indicationOfDataSegmentByteArray.length);
		if(!isEndOfFile)
			System.arraycopy(dataInSegment, 0, segment, (sequenceNumber.length + checksumForServer3ByteArray.length + indicationOfDataSegmentByteArray.length), dataInSegment.length);
		else if(isEndOfFile)
			System.arraycopy(dataInSegment, 0, segment, (sequenceNumber.length + checksumForServer3ByteArray.length + indicationOfDataSegmentByteArray.length), numberOfBytesReceived);
		
		datagramPacketForServer3 = new DatagramPacket(segment, segment.length, iNetAddressOfServer3, portNumberOfServer);
		datagramSocketForServer3.send(datagramPacketForServer3);
	}
	
	public void sendSegmentToServer4() throws IOException {
		checksumForServer4 = calculateUDPChecksum(iNetAddressOfServer4, sequenceNumberInBinary, indicationOfDataSegment, dataInSegment);
		
		checksumForServer4ByteArray[0] = (byte) ((checksumForServer4 >>> 8) & 0xFF);
		checksumForServer4ByteArray[1] = (byte) (checksumForServer4 & 0xFF);
		
		System.arraycopy(sequenceNumber, 0, segment, 0, sequenceNumber.length);
		System.arraycopy(checksumForServer4ByteArray, 0, segment, sequenceNumber.length, checksumForServer4ByteArray.length);
		System.arraycopy(indicationOfDataSegmentByteArray, 0, segment, (sequenceNumber.length + checksumForServer4ByteArray.length), indicationOfDataSegmentByteArray.length);
		if(!isEndOfFile)
			System.arraycopy(dataInSegment, 0, segment, (sequenceNumber.length + checksumForServer4ByteArray.length + indicationOfDataSegmentByteArray.length), dataInSegment.length);
		else if(isEndOfFile)
			System.arraycopy(dataInSegment, 0, segment, (sequenceNumber.length + checksumForServer4ByteArray.length + indicationOfDataSegmentByteArray.length), numberOfBytesReceived);
		
		datagramPacketForServer4 = new DatagramPacket(segment, segment.length, iNetAddressOfServer4, portNumberOfServer);
		datagramSocketForServer4.send(datagramPacketForServer4);
	}
	
	public void sendSegmentToServer5() throws IOException {
		checksumForServer5 = calculateUDPChecksum(iNetAddressOfServer5, sequenceNumberInBinary, indicationOfDataSegment, dataInSegment);
		
		checksumForServer5ByteArray[0] = (byte) ((checksumForServer5 >>> 8) & 0xFF);
		checksumForServer5ByteArray[1] = (byte) (checksumForServer5 & 0xFF);
		
		System.arraycopy(sequenceNumber, 0, segment, 0, sequenceNumber.length);
		System.arraycopy(checksumForServer5ByteArray, 0, segment, sequenceNumber.length, checksumForServer5ByteArray.length);
		System.arraycopy(indicationOfDataSegmentByteArray, 0, segment, (sequenceNumber.length + checksumForServer5ByteArray.length), indicationOfDataSegmentByteArray.length);
		if(!isEndOfFile)
			System.arraycopy(dataInSegment, 0, segment, (sequenceNumber.length + checksumForServer5ByteArray.length + indicationOfDataSegmentByteArray.length), dataInSegment.length);
		else if(isEndOfFile)
			System.arraycopy(dataInSegment, 0, segment, (sequenceNumber.length + checksumForServer5ByteArray.length + indicationOfDataSegmentByteArray.length), numberOfBytesReceived);
		
		datagramPacketForServer5 = new DatagramPacket(segment, segment.length, iNetAddressOfServer5, portNumberOfServer);
		datagramSocketForServer5.send(datagramPacketForServer5);
	}
	
	/*
	 * Depending on the number of servers in the program (in the range of 1 to 5 in our case),
	 * the client goes into the wait state in order to receive and process the ACK.
	 * 
	 * */
	public void handleAcknowledgements() throws IOException, InterruptedException {
		Timer timer = new Timer();
		TimeoutHandlingTask timeoutHandlingTask = new TimeoutHandlingTask();
		timer.schedule(timeoutHandlingTask, 20, 20);
		/*
		 * 1. This is important because if retransmission to a server is not 
		 *    taking place then it should not wait for ACK. 
		 *    The 'if(!isGoodToSendNextSegmentToServer1)' statement is used to handle retransmissions.
		 *    This condition will not put the client into ACK waiting state if it is already
		 *    good to send next segment to that particular server.
		 * 2. Initially, the flag corresponding to each server is false and hence the
		 *    client will go into the waiting state in order to receive an ACK from
		 *    all the servers. 
		 * */
		while(!isGoodToSendNextSegment) {
			Thread.sleep(7);
		}
		timer.cancel();
		sequenceNumberInDecimal++;
		isGoodToSendNextSegment = false;
		Iterator iterator = map.entrySet().iterator();
		   while (iterator.hasNext()) {
		       Map.Entry pair = (Map.Entry)iterator.next();
		       map.put((String)pair.getKey(),0);
		   }	
	}
	
	
	
	/*
	 * /*
	 * 1. Waits for ACK from server.
	 * 2. If the sequence numbers are equal and the other 16 bit fields satisfy the formats
	 *    then the client is good to send the next segment to this server atleast
	 *    and updates the record by setting the corresponding boolean flag to 'true'.
	 * 3. Once a segment is received, it separates 32 bit sequence number, 16 bit field of zeros 
	 *    that should be present in ACK, 16 bit indication that it is ACK packet and converts
	 *    each one into its decimal equivalent so as to make comparisons directly using 
	 *    the decimal values.
	 * 4. The server acknowledges the last correctly received segment. This means that if the 
	 *    latest segment sent was received correctly then the sequence no. in ACK is equal
	 *    to the sequence number of the sent segment.
	 * */
	public void readAndProcessAcknowledgement(String ipAddressOfServer) {
		receivedSequenceNumber[0] = String.format("%"+Integer.toString(8)+"s",Integer.toBinaryString(acknowledgement[0] & 0xFF)).replace(" ","0");
		receivedSequenceNumber[1] = String.format("%"+Integer.toString(8)+"s",Integer.toBinaryString(acknowledgement[1] & 0xFF)).replace(" ","0");
		receivedSequenceNumber[2] = String.format("%"+Integer.toString(8)+"s",Integer.toBinaryString(acknowledgement[2] & 0xFF)).replace(" ","0");
		receivedSequenceNumber[3] = String.format("%"+Integer.toString(8)+"s",Integer.toBinaryString(acknowledgement[3] & 0xFF)).replace(" ","0");
		receivedSequenceNumberInDecimal = Integer.parseInt(receivedSequenceNumber[0] + receivedSequenceNumber[1] + receivedSequenceNumber[2] + receivedSequenceNumber[3], 2);
		
		receivedFieldOfZeros[0] = String.format("%"+Integer.toString(8)+"s",Integer.toBinaryString(acknowledgement[4] & 0xFF)).replace(" ","0");
		receivedFieldOfZeros[1] = String.format("%"+Integer.toString(8)+"s",Integer.toBinaryString(acknowledgement[5] & 0xFF)).replace(" ","0");
		receivedFieldOfZerosInDecimal = Integer.parseInt(receivedFieldOfZeros[0] + receivedFieldOfZeros[1], 2);
		
		indicationOfAcknowledgementPacket[0] = String.format("%"+Integer.toString(8)+"s",Integer.toBinaryString(acknowledgement[6] & 0xFF)).replace(" ","0");
		indicationOfAcknowledgementPacket[1] = String.format("%"+Integer.toString(8)+"s",Integer.toBinaryString(acknowledgement[7] & 0xFF)).replace(" ","0");
		indicationOfAcknowledgementPacketInDecimal = Integer.parseInt(indicationOfAcknowledgementPacket[0] + indicationOfAcknowledgementPacket[1], 2);
		
		if(receivedSequenceNumberInDecimal == sequenceNumberInDecimal && receivedFieldOfZerosInDecimal == 0 && indicationOfAcknowledgementPacketInDecimal == Integer.parseInt("1010101010101010",2)) {
			map.put(ipAddressOfServer, 1);
			
			Iterator iterator = map.entrySet().iterator();
			int sumOfValuesInMap = 0;
			   while (iterator.hasNext()) {
			       Map.Entry pair = (Map.Entry)iterator.next();
			       sumOfValuesInMap += (int)pair.getValue();
		}
			   if(sumOfValuesInMap == map.size())
				   isGoodToSendNextSegment = true;
		}
	}
	
	public int calculateUDPChecksum(InetAddress ipAddressOfServer, String sequenceNumberInBinary, int indicationOfDataSegment, byte[] dataInSegment) throws UnknownHostException {
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
		
		sourceIPAddress = InetAddress.getLocalHost().getHostAddress();
		/*
		 * In order to get 16 bit binary strings for checksum calculation:
		 * 1. Splitting the IP address using '.' as the delimiter since IP address is in that form. 
		 * 2. Converting each byte of IP address into binary string of 8 bits.
		 * 3. Combining (1st, 2nd) and (3rd, 4th) binary strings to make it a binary string of 16 bits.
		 * 4. Adding these 16 bit strings to the sum.
		 * */
		splittedSourceIpAddress = sourceIPAddress.split(delimiter);
		firstByteInBinary = String.format("%"+Integer.toString(8)+"s",Integer.toBinaryString(Integer.parseInt(splittedSourceIpAddress[0]) & 0xFF)).replace(" ","0");
		secondByteInBinary = String.format("%"+Integer.toString(8)+"s",Integer.toBinaryString(Integer.parseInt(splittedSourceIpAddress[1]) & 0xFF)).replace(" ","0");
		thirdByteInBinary = String.format("%"+Integer.toString(8)+"s",Integer.toBinaryString(Integer.parseInt(splittedSourceIpAddress[2]) & 0xFF)).replace(" ","0");
		fourthByteInBinary = String.format("%"+Integer.toString(8)+"s",Integer.toBinaryString(Integer.parseInt(splittedSourceIpAddress[3]) & 0xFF)).replace(" ","0");
		sourceIpAs16BitString[0] = firstByteInBinary + secondByteInBinary;
		sourceIpAs16BitString[1] = thirdByteInBinary + fourthByteInBinary;
//		sum += Integer.parseInt(sourceIpAs16BitString[0],2) + Integer.parseInt(sourceIpAs16BitString[1],2);
		
		destinationIPAddress = ipAddressOfServer.getHostAddress();
		splittedDestinationIpAddress = destinationIPAddress.split(delimiter);
		firstByteInBinary = String.format("%"+Integer.toString(8)+"s",Integer.toBinaryString(Integer.parseInt(splittedDestinationIpAddress[0]) & 0xFF)).replace(" ","0");
		secondByteInBinary = String.format("%"+Integer.toString(8)+"s",Integer.toBinaryString(Integer.parseInt(splittedDestinationIpAddress[1]) & 0xFF)).replace(" ","0");
		thirdByteInBinary = String.format("%"+Integer.toString(8)+"s",Integer.toBinaryString(Integer.parseInt(splittedDestinationIpAddress[2]) & 0xFF)).replace(" ","0");
		fourthByteInBinary = String.format("%"+Integer.toString(8)+"s",Integer.toBinaryString(Integer.parseInt(splittedDestinationIpAddress[3]) &0xFF)).replace(" ","0");
		destinationIpAs16BitString[0] = firstByteInBinary + secondByteInBinary;
		destinationIpAs16BitString[1] = thirdByteInBinary + fourthByteInBinary;
//		sum += Integer.parseInt(destinationIpAs16BitString[0],2) + Integer.parseInt(destinationIpAs16BitString[1],2);
		/*
		 * protocol type number '17' represents UDP. Hence, it is used explicitly.
		 * Further, converting it to a 16 bit binary string.
		 * */
		protocolType = String.format("%"+Integer.toString(16)+"s",Integer.toBinaryString(17)).replace(" ","0");
		sum += Integer.parseInt(protocolType,2);
		/*
		 * The UDP segment length in bytes would be (maximumSegmentSize + 6) because 
		 * data in each segment would contain MSS number of bytes and additional 
		 * 6 bytes (4 bytes of UDP sequence number + 2 bytes of indication that this is a data packet).
		 * It does not include 2 bytes of UDP checksum because that is what is calculated here and 
		 * would then be attached and hence the UDP segment length should not comprise of UDP checksum length. 
		 */
		udpSegmentLength = String.format("%"+Integer.toString(16)+"s",Integer.toBinaryString((numberOfBytesReceived + 6) & 0xFFFF)).replace(" ","0");
		sum += Integer.parseInt(udpSegmentLength,2);
		
		/*
		 * Splitting the 32 bit sequence number into two strings of 16 bits each and then adding them to the sum.
		 * */
		sequenceNumberInBinaryAs16BitString[0] = sequenceNumberInBinary.substring(0, 16);
		sequenceNumberInBinaryAs16BitString[1] = sequenceNumberInBinary.substring(16, 32);
		sum += Integer.parseInt(sequenceNumberInBinaryAs16BitString[0],2) + Integer.parseInt(sequenceNumberInBinaryAs16BitString[1],2);
		if(!isEndOfFile)
			sum += indicationOfDataSegment;
		else if(isEndOfFile)
			sum += indicationOfLastDataSegment;
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
		if(numberOfBytesReceived % 2 == 0) {
			for(bytesTraversed = 0; bytesTraversed < numberOfBytesReceived; bytesTraversed += 2) {
				dataInSegmentInBinaryAs8BitString[0] = String.format("%"+Integer.toString(8)+"s",Integer.toBinaryString(dataInSegment[bytesTraversed] & 0xFF)).replace(" ","0");
				dataInSegmentInBinaryAs8BitString[1] = String.format("%"+Integer.toString(8)+"s",Integer.toBinaryString(dataInSegment[bytesTraversed + 1] & 0xFF)).replace(" ","0");
				sum += Integer.parseInt((dataInSegmentInBinaryAs8BitString[0] + dataInSegmentInBinaryAs8BitString[1]),2);
			}
		}
		else {
			for(bytesTraversed = 0; bytesTraversed < numberOfBytesReceived - 1; bytesTraversed += 2) {
				dataInSegmentInBinaryAs8BitString[0] = String.format("%"+Integer.toString(8)+"s",Integer.toBinaryString(dataInSegment[bytesTraversed] & 0xFF)).replace(" ","0");
				dataInSegmentInBinaryAs8BitString[1] = String.format("%"+Integer.toString(8)+"s",Integer.toBinaryString(dataInSegment[bytesTraversed + 1] & 0xFF)).replace(" ","0");
				sum += Integer.parseInt((dataInSegmentInBinaryAs8BitString[0] + dataInSegmentInBinaryAs8BitString[1]),2);
			}
			dataInSegmentInBinaryAs8BitString[0] = String.format("%"+Integer.toString(8)+"s",Integer.toBinaryString(dataInSegment[bytesTraversed] & 0xFF)).replace(" ","0");
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
		/*
		 * Checksum obtained by taking 1's complement of binary representation of sum. //This working was verified using 'testSum' above.//
		 * */
		checksum = String.format("%"+Integer.toString(16)+"s",Integer.toBinaryString(~sum & 0xFFFF)).replace(" ","0");
		return Integer.parseInt(checksum,2);
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		  Scanner readClientInput = new Scanner(System.in);
		  System.out.println("Please invoke client to initiate Point to Multipoint file transfer with n(1 to 5) servers using following format");
		  System.out.println("p2mpclient server-1 server-2..server-n server-port# file-name MSS");
		  String clientInput = readClientInput.nextLine();
		  Scanner scanner  = new Scanner(clientInput).useDelimiter(" ");
		  while(scanner.hasNext()) {
			  clientInputArrayList.add(scanner.next());
		  }
		if(clientInputArrayList.size() == 5) {
			Client client = new Client(clientInputArrayList.get(1), clientInputArrayList.get(2), clientInputArrayList.get(3), clientInputArrayList.get(4));
		}
		else if(clientInputArrayList.size() == 6) {
			Client client = new Client(clientInputArrayList.get(1), clientInputArrayList.get(2), clientInputArrayList.get(3), clientInputArrayList.get(4), clientInputArrayList.get(5));
		}
		else if(clientInputArrayList.size() == 7) {
			Client client = new Client(clientInputArrayList.get(1), clientInputArrayList.get(2), clientInputArrayList.get(3), clientInputArrayList.get(4), clientInputArrayList.get(5),
								clientInputArrayList.get(6));
		}
		else if(clientInputArrayList.size() == 8) {
			Client client = new Client(clientInputArrayList.get(1), clientInputArrayList.get(2), clientInputArrayList.get(3), clientInputArrayList.get(4), clientInputArrayList.get(5), 
								clientInputArrayList.get(6), clientInputArrayList.get(7));
		}
		else if(clientInputArrayList.size() == 9) {
			Client client = new Client(clientInputArrayList.get(1), clientInputArrayList.get(2), clientInputArrayList.get(3), clientInputArrayList.get(4), clientInputArrayList.get(5), 
								clientInputArrayList.get(6), clientInputArrayList.get(7), clientInputArrayList.get(8));
		}
		
	}

}
