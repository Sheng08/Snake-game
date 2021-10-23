import java.awt.BorderLayout;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;


public class Server extends JFrame {
	/**
	 * Create by ShengPo
	 */
	private static final long serialVersionUID = 1L;
	private JTextArea txt = new JTextArea("���A���w�g�Ұ�\r\n");//�����ƻ򤣷|���� \n\r�]�@��
	private ServerSocket serverSocket = null;
	ExecutorService exec= null;
	private Scorce sscorce = new Scorce();
	private List<Socket> clients;
	Map<Integer, Integer> ClientScorce = new LinkedHashMap<Integer,Integer>();	

	

	
	PrintWriter pw;
	PrintWriter ppw;
	int EGG_X = 404;
	int EGG_Y = 380;
	public Server() throws IOException{
		txt.setLineWrap(true); //�E���۰ʴ���\��
		setLayout(new BorderLayout());
		this.add(new JScrollPane(txt),BorderLayout.CENTER);
		setSize(500,300);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
		try {
			clients = new ArrayList<Socket>();
			System.out.println("Server�Ұʦ��\");
			int count=1; //�p��ϥΪ�

			serverSocket = new ServerSocket(54321);
			exec = Executors.newCachedThreadPool();
			exec.execute(sscorce);
			while (true){//�ƻ�ɭԶ�����?
				Socket socket = null;
				socket = serverSocket.accept();
				System.out.println("Client "+socket.getPort()+" Enter");
				
				clients.add(socket);
				
				for (int i = clients.size() - 1; i >= 0; i--) {
					ppw = new PrintWriter(clients.get(i).getOutputStream(), true);
					for(int j=count; j>0;j--){
						ppw.println("Server:Player:"+ (j));
						System.out.println("Server:Player:"+ (j));
					}
					
				}
				if(count == 1) {
					pw = new PrintWriter(clients.get(count-1).getOutputStream(), true);
					pw.println("Server:EGG:"+EGG_X+":"+EGG_Y);//�o�e��l��m TODO �g�H����m
					pw.flush();
					
				}else{
					pw = new PrintWriter(clients.get(count-1).getOutputStream(), true);
					pw.println("Server:EGG:"+EGG_X+":"+EGG_Y);//�o�e��l��m TODO �g�H����m
					pw.flush();
				}
				ppw.flush();
				ClientScorce.put(count, 0);
	    		exec.execute(new ClientListener(socket, count));//�ΰ�������ҰʳQ�ʦ���Client�ݸ�TThread
				count++;
			}
		} catch (Exception e) {
			System.out.println("Server�Ұʥ���! ERROR!");
			System.out.println(e);
		}
	}

	//��ť���a�ʺA������(�Y�J ��V)�åB���s��
	public class ClientListener implements Runnable {
		private Socket socket;
		boolean flag = false;
		private String msg;

		private int count;
		private BufferedReader br;
		private PrintWriter pw;
		private Move move;
		int Play_Score = 0;
		boolean isPaused = false;
		
		public ClientListener(Socket socket,int count) throws IOException{

			this.socket = socket; //���w�Ө�ܸ̭����ܼ� private Socket socket; ��this �N���Φh�g�ܼƦW��
			this.count = count; //�|���|�X���D �]��private?��this �άۦP�M�ƦW �ѼƩM�K�O�W�٤@��

			msg ="["+socket.getPort()+ "]�w�s�u����A��\n";
			txt.append(msg);
			// sendMessage();
			msg="";
			move = new Move(socket, count);
			move.play1_X= (404 - 300 + ((int)(Math.random() * 15 * 2)) * 20);
			move.play1_Y= (380 - 300 + ((int)(Math.random() * 15 * 2)) * 20);
			exec.execute(move);//�ΰ�������Ұʪ��a���ʱ���Thread
		}
		
		@Override
		public void run() {
			try {
		    	br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		        msg = "Player"+ count + "�i�J�C���ǡI" ;
		        Broadcast(msg);
		        while ( ( msg = br.readLine()) != null) {   //����Client�ݸ�T
		        	msg = "Player" + count + ":" + msg;
		        	Broadcast(msg);  //�s���A�]�������V Player1�M2���n����
		          
		        	String control[] = msg.split(":"); 
		        	System.out.println("msg:"+msg);
		        	
		        	//Play�Y��J
		        	if(control[1].equals("EGG") && control[2].equals("EAT")) {
						EGG_X = (404 - 300 + ((int)(Math.random() * 15 * 2)) * 20);
						EGG_Y = (380 - 300 + ((int)(Math.random() * 15 * 2)) * 20);
		        		String rand = "Server:EGG:" + EGG_X + ":" + EGG_Y;

		        		for (int i = clients.size() - 1; i >= 0; i--) {
		        			PrintWriter pw = new PrintWriter(clients.get(i).getOutputStream(), true);
				    		pw.println(rand);
							System.out.println(rand);
						}
		        		if(control[3].equals(String.valueOf(count)) ) { //�w�藍�P�D
		        			Play_Score++;
							ClientScorce.put(count, Play_Score);

		        			for (int i = clients.size() - 1; i >= 0; i--) {
			        			PrintWriter pw = new PrintWriter(clients.get(i).getOutputStream(), true);
			        			// pw.println("Server:Score:"+count+":" + ClientScorce.get(count));
								pw.println("Server:Score:"+count+":" + Play_Score);
		        			}
		        			System.out.println("Server:Score:"+count+":" + Play_Score);
		        		}
		        	}
		        	if(control[1].equals("Speed") && control[2].equals(String.valueOf(count))) { //�w�藍�P�D
						
						move.Speed = Integer.parseInt( control[3] );
						System.out.println("Server:Speed:"+count+":" + Play_Score);
					}
		        	//��V
					if(control[1].equals("Direction")) {
						if(control[2].equals("UP")) {
							move.Play1_State = "UP";
						}else if(control[2].equals("DOWN")) {
							move.Play1_State = "DOWN";
						}else if(control[2].equals("LEFT")) {
							move.Play1_State = "LEFT";
						}else if(control[2].equals("RIGHT")) {
							move.Play1_State = "RIGHT";
						}
					}
		        	if(control[1].equals("GAMEOVER")&& control[2].equals(String.valueOf(count))) {
						System.out.println(control[0]+":"+control[1]);
						move.setHead(384, 360);
						move.StartGame = false;
						move.Step = 0;
						for (int i = clients.size() - 1; i >= 0; i--) {
							PrintWriter pw = new PrintWriter(clients.get(i).getOutputStream(), true);
							pw.println("Server:GAMEOVER:"+count);
						}
		        	}else if (control[1].equals("GAMESTART")&& control[2].equals(String.valueOf(count))){
						System.out.println(control[0]+":"+control[1]);
						Play_Score = 0;
						
						exec.execute(move);
						move.Step = 20;
						move.Speed = 1000;
						move.StartGame = true;
						move.setHead((404 - 300 + ((int)(Math.random() * 15 * 2)) * 20), (380 - 300 + ((int)(Math.random() * 15 * 2)) * 20));
						move.Play1_State=  "DOWN";
					}else if (control[1].equals("FIRST")&& control[2].equals(String.valueOf(count))){
						System.out.println(control[0]+":"+control[1]);
						if (flag==false){
							Play_Score = 0;
						
							// exec.execute(move); //����@����Enter�|�@����
							move.Step = 20;
							move.Speed = 1000;
							move.StartGame = true;
							move.Play1_State=  "DOWN";
							flag = true;

						}
						

					}else if (control[1].equals("STOP")&& control[2].equals(String.valueOf(count))){
						System.out.println(control[0]+":"+control[1]);
						isPaused = !isPaused;
						if (isPaused==true){
							move.Step = 0;
						}else{
							move.Step = 20;
						}
					}
		        	
		        }
	    	}catch (Exception e) {
	    		
				// socket.close();
				System.out.println("ClientListener "+socket.getPort()+count+" is close!");//�g�bGUI //�M��player count ���O�H�Υ�Linklist??
				txt.append("["+socket.getPort()+"] Player "+count+" �w����!\n");
				move.StartGame = false;
				System.out.println(e);
	    	}
			
		}
		public void Broadcast(String Msg) {
	    	try {
	    		System.out.println(Msg);
		    	for (int i = clients.size() - 1; i >= 0; i--) {
		    		pw = new PrintWriter(clients.get(i).getOutputStream(), true);
		    		pw.println(Msg);
		    		pw.flush();
		    	}
	    	} catch (Exception e) {
	    		System.out.println(e);
	    	}
	    }
	}

	//���a���ʱ��� �w������
	public class Move implements Runnable{
		// private Socket socket;
		Socket ssocket;
		int ccount;
		int Speed = 1000;
		boolean StartGame = true;
		int Step = 0;
		// String Play1_State = "";
		String Play1_State = "DOWN";//�@�}�l�w�]��V
		int play1_X = 0; int play1_Y = 0;//�i�H�����l��m
		public Move(Socket socket, int count){
			this.ssocket = socket;
			this.ccount = count;

		}
		public void setHead(int x, int y){
			this.play1_X = x;
			this.play1_Y = y;
		}
	
		
		@Override
		public void run() {
			//�e���n�]�wStartGame�ܼ� ��public�٬Oprivate?
			//��g�h�H
			while(StartGame == true) {
				try {

					//���a�@
			        if(Play1_State == "UP") {
			        	play1_Y-=Step;
			        	for (int i = clients.size() - 1; i >= 0; i--) {
		        			PrintWriter pw = new PrintWriter(clients.get(i).getOutputStream(), true);
				    		pw.println("Server:Move:"+ccount+":" + play1_X + ":" + play1_Y+":1");
							System.out.println("Server:Move:"+ccount+":" + play1_X + ":" + play1_Y+":1");
						}
			        }else if(Play1_State == "DOWN") {
			        	play1_Y+=Step;
			        	for (int i = clients.size() - 1; i >= 0; i--) {
		        			PrintWriter pw = new PrintWriter(clients.get(i).getOutputStream(), true);
				    		pw.println("Server:Move:"+ccount+":" + play1_X + ":" + play1_Y+":2");
							System.out.println("Server:Move:"+ccount+":" + play1_X + ":" + play1_Y+":2");
						}
			        }else if(Play1_State == "LEFT") {
			        	play1_X-=Step;
			        	for (int i = clients.size() - 1; i >= 0; i--) {
		        			PrintWriter pw = new PrintWriter(clients.get(i).getOutputStream(), true);
				    		pw.println("Server:Move:"+ccount+":" + play1_X + ":" + play1_Y+":3");
							System.out.println("Server:Move:"+ccount+":" + play1_X + ":" + play1_Y+":3");
						}
			        }else if(Play1_State == "RIGHT") {
			        	play1_X+=Step;
			        	for (int i = clients.size() - 1; i >= 0; i--) {
		        			PrintWriter pw = new PrintWriter(clients.get(i).getOutputStream(), true);
				    		pw.println("Server:Move:"+ccount+":" + play1_X + ":" + play1_Y+":4");
							System.out.println("Server:Move:"+ccount+":" + play1_X + ":" + play1_Y+":4");
						}
			        }

					Thread.sleep(Speed);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public class Scorce implements Runnable{
		int ClientScorceVal;
		PrintWriter pw;
		int firstKey;
		public Scorce(){
		}

		@Override
		public void run() {

			while(true) {
				Set<Map.Entry<Integer, Integer>> companyFounderSet = ClientScorce.entrySet();
				List<Map.Entry<Integer, Integer>> companyFounderListEntry = new ArrayList<Map.Entry<Integer, Integer>>(companyFounderSet);
				Collections.sort(companyFounderListEntry, new Comparator<Map.Entry<Integer, Integer>>() {
					@Override
					public int compare(Entry<Integer, Integer> es1, 
							Entry<Integer, Integer> es2) {
						return es2.getValue().compareTo(es1.getValue());
					}
				});
				ClientScorce.clear();
				int count = 1;
				for(Map.Entry<Integer, Integer> map : companyFounderListEntry){
					if (count == 1) {
						firstKey = map.getKey();
					}

					ClientScorce.put(map.getKey(), map.getValue());
					count++;
				}
				Set<Integer> keys = ClientScorce.keySet();
				List<Integer> listKeys = new ArrayList<Integer>( keys );

				try {
				
					for (int i = 0; i < clients.size(); i++) {
						pw = new PrintWriter(clients.get(i).getOutputStream(), true);
						pw.println("Server:PlayersScorce:"+(listKeys.indexOf(i+1)+1)+":"+firstKey);
					}
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	
	public static void main(String[] args) throws IOException {
		Server server = new Server();
	}
}