# CLIBasedDownloader
 File downloader which uses multithreading


Steps to install the tool and run

Step-1) Clone the repository
Step-2) Install maven using  'mvn install'
Step-3) run using -
java -jar ./IllumionCLIBasedDownloader-1.0-SNAPSHOT.jar <URL> <NUMBER OF THREADS>


 PART-1: Dividing the Large File into Small Parts

 The code uses the concept of multithreading and concurrent programming. Given the number of threads, the code will divide the file into (size of file)/(number of threads user inputs). The 'n' parts then created download the file in concurrent manner thereby taking less time to download the file. After 'n' parts are downloaded successfully, the code then merges n part into one single file by joining all the parts together.

 PART-2: Connection Fails, Interruption and Performance

 1) If for some reason, connection fails or is interrupted, the code for now halts and waits for the server to connect.
 2) The code checks number of cores(virtual cores in this context) the user's system has, and if user tries to input out of range number of threads, the program will throw an exception, asking the user to input the number of threads in given range to maximize efficiency.

 We select virtual cores to apply the concept of Hyperthreading. Maximum efficiency can be achieved if the number of threads we use in our code are equal to number of cores the system contains.

 3)There will be no deadlock or bottleneck when it comes to threads as we are splitting our file size into n equal parts and each thread's job is to download that specific part of the file only.

 4) This code can scale but upto a certain point. As the user will go on increasing the number of threads to be used, the performance will start deteriorating after it reaches a threshold value(with threshold value being 2*number of cores system contains).


 Future work:
 1) If the file fails to download fully, the code will check if the file exists already and then tries to download the remaining part, thus saving the user a lot of time.
 2) The code as of now can not download zip files. Later versions of the code will include zip file compatibility as well.
 
