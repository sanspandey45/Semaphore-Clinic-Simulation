
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class Clinic {
	
	private static AtomicInteger patientsDone = new AtomicInteger(0);
	static int numOfDocs;
	static int numOfPats;
	
	// Static semaphores used across threads
	static Semaphore reception = new Semaphore(1, true);
	static Semaphore mutex1 = new Semaphore(1, true);
	static Semaphore[] patWaiting = new Semaphore[3]; // for nurse
	static Semaphore[] patInOffice = new Semaphore[3]; // for doctor
	static Semaphore[] officeFree = new Semaphore[3];
	static Semaphore[] readyForPatient = new Semaphore[3];
	static Semaphore[] patRegistered = new Semaphore[15];
	static Semaphore[] patLeftReception = new Semaphore[15];
	static Semaphore[] advisedByDoc = new Semaphore[15];
	static Semaphore[] finished = new Semaphore[15];
	//static Semaphore[] nurseSem = new Semaphore[3];

	
	// Other variables used across thread:
	static BlockingQueue<Integer> recPatIdQueue = new LinkedBlockingQueue<>();
	static BlockingQueue<Integer> recDocRequestQueue = new LinkedBlockingQueue<>();
	@SuppressWarnings("unchecked")
	static BlockingQueue<Integer>[] nursePatientQueues = new ArrayBlockingQueue[3];
	//static BlockingQueue<Integer> nursePatIdQueue = new LinkedBlockingQueue<>();
	//static BlockingQueue<Integer> nurseDocRequestQueue = new LinkedBlockingQueue<>();
	//static BlockingQueue<Integer>[] doctorQueues = new ArrayBlockingQueue[3];
	

	// Initializing semaphore arrays and blocking queues before main so accessible by all without passing
	public static void initialize()
	{
		for (int i = 0; i < 3; i++)
		{
			patWaiting[i] = new Semaphore(0, true);
			patInOffice[i] = new Semaphore(0, true);
			officeFree[i] = new Semaphore(1, true);
			readyForPatient[i] = new Semaphore(0, true);
			nursePatientQueues[i] = new ArrayBlockingQueue<>(15);
			//nurseSem[i] = new Semaphore(1, true);
			
		}
		
		for (int i = 0; i < 15; i++)
		{
			patRegistered[i] = new Semaphore(0, true);
			patLeftReception[i] = new Semaphore(0, true);
			advisedByDoc[i] = new Semaphore(0, true);
			finished[i] = new Semaphore(0, true);
		}
			
	}
	
	
	public static void main (String[] args) 
	{
		if (args.length < 2) 
		{
            System.err.println("Please provide two integer inputs- number of doctors and number of patients.");
            System.exit(1); // Exit the program with an error code
        }
		
		numOfDocs = Integer.parseInt(args[0]);
		numOfPats = Integer.parseInt(args[1]);
		
		initialize(); // call static initialize function to initialize semaphore arrays and queues.
		
		// Creating patient threads
		Thread patient[] = new Thread[numOfPats];
		
		for (int i = 0; i < numOfPats; i++)
			patient[i] = new Thread(new Patient(i, numOfPats, numOfDocs));
		
		
		// Creating doctor threads:
	
		Thread doctorThr[] = new Thread[numOfDocs]; // Array of doc threads
		Doctor doctors[] = new Doctor[numOfDocs]; // Array of doc objects
		
		for (int i = 0; i < numOfDocs; i++)
		{
			doctors[i] = new Doctor(i);
			doctorThr[i] = new Thread(doctors[i]);
		}
		
		// Creating nurse threads
		Thread nurseThr[] = new Thread[numOfDocs]; // Array of nurse threads
		//Nurse nurses[] = new Nurse[numOfDocs]; // Array of nurse objects
		for (int i = 0; i < numOfDocs; i++)
		{
			// Each nurse object gets passed the same array of doc objects
			//nurses[i] = new Nurse(i, doctors); // Populate first nurse object
			nurseThr[i] = new Thread(new Nurse(i, doctors));
		}
		
		// Creating receptionist thread
		Receptionist receptionist = new Receptionist();
		Thread recepThr = new Thread(receptionist);
		
		System.out.println("Run with " + numOfPats + " patients, " + numOfDocs 
				+ " nurses, " + numOfDocs + " doctors\n");
		
		// Starting all threads:
		for (int i = 0; i < numOfPats; i++)
			patient[i].start();
		
		recepThr.start();
		
		for (int i = 0; i < numOfDocs; i++)
		{
			nurseThr[i].start();
			doctorThr[i].start();
		}
	}
	
	public static class Patient implements Runnable 
	{
		int pat_id;
		Random random = new Random();
		int doc_request;
		int totalPats;
		int totalDocs;
		
		//BlockingQueue<Integer> docRequestQueue;
		
		public Patient (int pat_id, int totalPats, int totalDocs)/*, BlockingQueue<Integer> docRequestQueue*/
		{
			this.pat_id = pat_id;
			this.totalPats = totalPats;
			this.totalDocs = totalDocs;
			//this.docRequestQueue = docRequestQueue;
		}
		
		
		@Override
		public void run() 
		{
			doc_request = random.nextInt(totalDocs);
			System.out.println("Patient " + pat_id + " enters waiting room, waits for receptionist");
			
			try
			{
				reception.acquire();
				
				//mutex1.acquire(); // Semaphore, not mutex (allows one access of queue at a time)
				recPatIdQueue.put(pat_id);
				recDocRequestQueue.put(doc_request);
				//mutex1.release();
				
				patRegistered[pat_id].acquire(); // Reception lets them know they're registered
				System.out.println("Patient " + pat_id + " leaves receptionist and sits in waiting room");
				patLeftReception[pat_id].release(); // Let nurse know they have left reception.
				reception.release(); // Let next patient know reception is free.
				
				readyForPatient[doc_request].acquire(); // Waiting for nurse to call
				
				advisedByDoc[pat_id].acquire(); // Waiting for doctor's advice.
				System.out.println("Patient " + pat_id + " receives advice from doctor " + doc_request);
				
				finished[pat_id].acquire(); // Wait for doctor to listen, advise, and release;
				System.out.println("Patient " + pat_id + " leaves ");
				
				if(patientsDone.getAndIncrement()+1 == totalPats)
				{
					System.out.println("Simulation complete");
					System.exit(0);
				}
				//mutex1.release();
			}
			
			catch (InterruptedException e) 
			{
				e.printStackTrace();
			}
			
		}

	}
	
	public static class Receptionist implements Runnable 
	{
		int r_pat_id;
		int r_doc_request;
		
		public Receptionist( )
		{
			//this.docRequestQueue = docRequestQueue;
			//this.nurses = nurses;
		}
		
		@Override
		public void run() 
		{
			while (true)
			{
				try 
				{
					//mutex1.acquire();
					r_pat_id = recPatIdQueue.take();
					r_doc_request = recDocRequestQueue.take();
					//mutex1.release();
					
					mutex1.acquire();
					nursePatientQueues[r_doc_request].put(r_pat_id); // Communicate pat id to correct nurse
					System.out.println("Receptionist registers patient " + r_pat_id);
					patRegistered[r_pat_id].release();
					mutex1.release();
					
					patWaiting[r_doc_request].release(); // Alert them another patient is waiting
					//System.out.println("Permits available after release: " + reception.availablePermits());
				} 
				catch (InterruptedException e) 
				{
					e.printStackTrace();
				}
			}
			
			
		}
		
	}
	
	public static class Nurse extends Thread 
	{
		int nurse_id;
		Doctor[] doctors;
		int n_pat_id;
		
		public Nurse (int nurse_id, Doctor[] doctor) 
		{
			this.nurse_id = nurse_id; // Same as doc id
			this.doctors = doctor; // Has all doctor objects accessible
		}

		@Override
		public void run() {
			while (true)
			{
				try 
				{
					// Alerted by reception, means at least 1 is waiting for this specific nurse
					// Also means the receptionist has already updated this nurse's n_pat_id.
					patWaiting[nurse_id].acquire();
					
					
					mutex1.acquire(); // So the receptionist doesn't add while nurse takes
					n_pat_id = nursePatientQueues[nurse_id].take(); // got from receptionist
					mutex1.release();
					
					patLeftReception[n_pat_id].acquire(); // Nurse makes sure patient is in waiting room, not reception
					
					// Check if office free:
					officeFree[nurse_id].acquire();
					
					// Alert patient to come in:
					readyForPatient[nurse_id].release();
					
					doctors[nurse_id].d_pat_id = n_pat_id; // Give patient to doctor
					System.out.println("Nurse " + nurse_id + " takes patient " + n_pat_id + " to doctor " + nurse_id + "'s office");
					
					patInOffice[nurse_id].release(); // Alert doctor
					
					// Now Does it all over with next in their queue
					
				} 
				catch (InterruptedException e) 
				{
					e.printStackTrace();
				}
			}
		}
	}


	public static class Doctor implements Runnable 
	{
		int doc_id;
		int d_pat_id;
		
		public Doctor (int doc_id) 
		{
			this.doc_id = doc_id;
			//this.patientQueue = new LinkedList<>();
		}
	
		@Override
		public void run() {
			while (true)
			{
				try 
				{
					patInOffice[doc_id].acquire();
					System.out.println("Doctor " + doc_id + " listens to symptoms from patient " + d_pat_id);
					advisedByDoc[d_pat_id].release(); // Give patient the advice. 
					
					finished[d_pat_id].release(); 
					officeFree[doc_id].release();
				}
				catch (InterruptedException e) 
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	
}
