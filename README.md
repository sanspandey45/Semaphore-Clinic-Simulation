# README


## About the Project

This project simulates a visit to the doctor’s office using Semaphores. The clinic to be simulated has doctors, each of which has their own nurse. Each doctor has an office of his or her own in which to visit patients.
Patients will enter the clinic to see a doctor, which is randomly assigned. Initially, a patient enters the waiting room and waits to register with the receptionist. Once registered, the patient sits in the waiting room until the nurse calls.  
The receptionist lets the nurse know a patient is waiting. The nurse directs the patient to the doctor’s office and tells the doctor that a patient is waiting. The doctor visits the patient and listens to the patient’s symptoms.  
The doctor advises the patient on the action to take. The patient then leaves.

### Threads
Receptionist – one thread   <br>
Doctor – one thread each, maximum of 3 doctors   <br>
Nurse – one per doctor thread, identifier of doctor and corresponding nurse should match   <br>
Patient – one thread each, up to 15 patients

### Inputs
The program receives number of doctors and patients as command-line inputs. (Number of nurses is the same as number of doctors.)

## How to build and run:
Compile: javac Clinic.java   <br>
Run: java clinic 3 15
