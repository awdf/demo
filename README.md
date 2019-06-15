#demo
Simple wallet client-server realization. The main idea - "no magic!"
###Technologies stack
    • Java 8+ 
    • Spring 5.x
    • gRPC
    • MySQL 5.x
    • Gradle
    • JUnit
    • Hibernate
    • Spring Boot 2.x
###How to run
    • Download the repository code
    • Import **demo.sql** to your MySQL 5.x DB on localhost:3306
    • In console run    
`
./gradlew bootRun --args="U10 C5 R10"
` 
    
#####Where:
* U10 - Client will emulate 10 users
* C5  - Amount of threads per user
* R10 - Sequence of single thread rounds

#####Rounds
Decision about which round will be used makes randomly. There is 3 predefined Round sequences:

* A 
    * Deposit 100 USD
    * Withdraw 200 USD
    * Deposit 100 EUR
    * Get Balance
    * Withdraw 100 USD
    * Get Balance
    * Withdraw 100 USD
* B
    * Withdraw 100 GBP
    * Deposit 300 GPB
    * Withdraw 100 GBP
    * Withdraw 100 GBP
    * Withdraw 100 GBP
* C
    * Get Balance
    * Deposit 100 USD
    * Deposit 100 USD
    * Withdraw 100 USD
    * Depsoit 100 USD
    * Get Balance
    * Withdraw 200 USD
    * Get Balance
    
###Interactions
Let me tell you how it works inside. At start of application creates Spring Boot application context _com.example.demo.WalletServer_
with gRPC server as communication service _com.example.demo.services.Impl.GrpcServerServiceImpl_. 

In the same time client _com.example.demo.WalletClient_ wait until server event _ApplicationReadyEvent_ fired. On the event 
client work starts. 

Client creates U x C threads for users work and fill it with rounds randomly. You can see total amount of planed actions for user
with ID in log, it's:

`User ID:4129 action list length 1324`

When all client interactions is done, you can see both time spent and total actions have sent:

* `Threads execution time 1021 ms`
* `Users planned actions 66658`

After this, you will see server total calculation of messages, it has processed:

`gRPC service processed 66658 messages`

That is all.

###Testing
You can do tests by shell command:

`./gradlew check`