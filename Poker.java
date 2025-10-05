import java.net.Socket;
import java.util.Arrays;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;

public class Poker{
    static DataInputStream dis;
    static DataOutputStream dos;

    /*
     * purpose: method for sending commmnads to Professor's dealer
     * author: Paul Haskell
     */
    private static void write(String s) throws IOException {
        dos.writeUTF(s);
        dos.flush();
    }

    /*
     * purpose: method for reading commmnads from Professor's dealer
     * author: Paul Haskell
     */
    private static String read() throws IOException {
        return dis.readUTF();
    }

    public static void main(String[] args) {
        
        try{
            Socket socket = new Socket(args[0], Integer.parseInt(args[1]));
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
            Cards cards = new Cards();
            boolean done = false;
            //int winCounter = 0;
            //int loseCounter = 0;

            while(!done){
                String[] dealerCommand = read().split(":");

                if(dealerCommand[0].equals("login")){

                    write("DemetriusChatterjee:Demetrius");

                }else if(dealerCommand[0].equals("bet1")){
                    
                    //decoding the bet1 message recieved into respective variables to start computations in order to decide whether to bet and how much or not to bet

                    int myChips = Integer.parseInt(dealerCommand[1]);
                    int minimumBet = Integer.parseInt(dealerCommand[3]);
                    String holeCard = dealerCommand[4];
                    String firstUpCard = dealerCommand[5];

                    //getting all other players' displayed cards (without my own being in it)
                    String[] allOtherPlayersFirstCards = new String[dealerCommand.length - 7];
                    boolean removedOwnCard = false;

                    for(int i = 7; i < dealerCommand.length; i++){
                        if(!removedOwnCard && dealerCommand[i].equals(firstUpCard)){
                            removedOwnCard = true;
                        }else{
                            allOtherPlayersFirstCards[i-7] = dealerCommand[i];
                        }
                    }

                    //doing the computation
                    // adding both cards
                    cards.addToMyOwnCards(holeCard);
                    cards.addToMyOwnCards(firstUpCard);
                    //evaluating my cards
                    String myEvaluatedCardsResults = cards.evaluateMyCards();
                    //deciding whether to make bet and if yes, how much
                    String betResults = cards.shouldBetForBet1(minimumBet, myChips, allOtherPlayersFirstCards, myEvaluatedCardsResults);
                    if(betResults.charAt(0) == 't'){
                        String betToMake = betResults.substring(1, betResults.length());
                        write("bet:"+betToMake);
                    }else{
                        write("fold");
                    }
                   
                }else if(dealerCommand[0].equals("bet2")){

                    //decoding the bet2 message recieved into respective variables to start computations in order to decide whether to bet and how much or not to bet

                    int myChips = Integer.parseInt(dealerCommand[1]);
                    int minimumBet = Integer.parseInt(dealerCommand[3]); 
                    String holeCard = dealerCommand[4];
                    String firstUpCard = dealerCommand[5];
                    String secondUpCard = dealerCommand[6];

                    //getting all other players' first and second cards (without my own 2 being in it)
                    String[] allOtherPlayersFirstCards = new String[(dealerCommand.length - 8)/2];
                    String[] allOtherPlayersSecondCards = new String[(dealerCommand.length - 8)/2];
                    boolean removedOwnCards = false;
                    int ownCardRemovedCounter = 0;

                    for(int i = 8; i < dealerCommand.length; i++){
                        if(!removedOwnCards && (dealerCommand[i].equals(firstUpCard) || dealerCommand[i].equals(secondUpCard))){
                            removedOwnCards = true;
                            ownCardRemovedCounter++;
                        }else{
                            if(i%2==1){
                                allOtherPlayersSecondCards[i-8] = dealerCommand[i];
                            }else{
                                allOtherPlayersFirstCards[i-8] = dealerCommand[i];
                            }   
                        }
                        if(ownCardRemovedCounter < 2){
                            removedOwnCards = false;
                        }
                    }

                    //doing the computation, continuation from bet1 object cards so only new card needs to be added
                    // adding both cards
                    cards.addToMyOwnCards(secondUpCard);
                    //evaluating my cards again
                    String myEvaluatedCardsResults = cards.evaluateMyCards();
                    //evaluating other players' two cards
                    String[] allOtherPlayerEvaluatedCards = cards.evaluateOthersCards(allOtherPlayersFirstCards, allOtherPlayersSecondCards);
                    //deciding whether to make bet and if yes, how much
                    String betResults = cards.shouldBetForBet2(minimumBet, myChips, allOtherPlayerEvaluatedCards, myEvaluatedCardsResults);
                    if(betResults.charAt(0) == 't'){
                        String betToMake = betResults.substring(1, betResults.length());
                        write("bet:"+betToMake);
                    }else{
                        write("fold");
                    }
                }else if(dealerCommand[0].equals("done")){

                    done = true;

                }else if(dealerCommand[0].equals("status")){
                    // DO NOTHING

                    //Woud have collected some stats about my win lose situation but that is not required

                    /*collecting stats when each game done
                    if(dealerCommand[1].equals("win")){
                        winCounter++;
                    }else{
                        loseCounter++;
                    }*/
                }

                if(done){
                    //int total = winCounter + loseCounter;
                    //System.out.println("Wins:"+winCounter+"/"+total);
                    socket.close();
                    System.exit(0);
                }
            }
        //incase there is an error in recieving or sending commands like a network error
        }catch( IOException ioe){
            System.err.println(ioe);
        }
    }
}

class Cards{
    String[] cardOrder = {"A", "K", "Q", "J", "10", "9", "8", "7", "6", "5", "4", "3", "2"};
    String[] myOwnCards = {"","",""};

    /*
     * purpose: method that adds a card to my card deck called myOwnCards
     * author: Demetrius Chatterjee
     */
    public void addToMyOwnCards(String myCard){
        for(int i = 0;i<3;i++) {
            if (!myOwnCards[i].equals("")) {
                myOwnCards[i] = myCard.substring(0, myCard.length()-1);
            }
        }
    }

    /*
     * purpose: goes through myOwnCards and returns how many of a kind I have and my highest card as one String 
     * author: Demetrius Chatterjee
     */
    public String evaluateMyCards(){
        int sameKindCounter = 1;
        if(myOwnCards[0].equals(myOwnCards[1]) && myOwnCards[0].equals(myOwnCards[2])){
            sameKindCounter = 3;
        }
        else if(myOwnCards[2].equals(myOwnCards[0]) && myOwnCards[2].equals(myOwnCards[1])){
            sameKindCounter = 3;
        }
        else{
            if(myOwnCards[0].equals(myOwnCards[1]) || myOwnCards[0].equals(myOwnCards[2])){
                sameKindCounter = 2;
            }
            else if(myOwnCards[2].equals(myOwnCards[0]) || myOwnCards[2] .equals(myOwnCards[1])){
                sameKindCounter = 2;
            }
        }
        return sameKindCounter + "" + HighestCard(myOwnCards);
    }

    /*
     * purpose: evaluates all other players two known cards and stores the highest and how many of a kind they have in allOtherPlayerEvaluatedCards and returns that array
     * author: Demetrius Chatterjee
     */
    public String[] evaluateOthersCards(String[] allOtherPlayersFirstCards, String[] allOtherPlayersSecondCards){ 
        String[] allOtherPlayerEvaluatedCards = new String[allOtherPlayersFirstCards.length];
        for(int i = 0;i<allOtherPlayerEvaluatedCards.length;i++){
            int sameKindCounter = 1;
            if(allOtherPlayersFirstCards[i].charAt(0) != '1'){
                if(allOtherPlayersFirstCards[i].equals(allOtherPlayersSecondCards[i])){
                    sameKindCounter = 2;
                }
            }else{
                if(allOtherPlayersFirstCards[i].substring(0, 2).equals("10") && allOtherPlayersSecondCards[i].substring(0,2).equals("10")){
                    sameKindCounter = 2;
                }
            }
            allOtherPlayerEvaluatedCards[i] = sameKindCounter + "" + HighestCard(allOtherPlayersFirstCards[i], allOtherPlayersSecondCards[i]);
        }
        return allOtherPlayerEvaluatedCards;
    }
    
    /*
     * purpose: returns a 't' followed by the bet to make otherwise 'f' followed by 0, computes whether to make a bet for bet1
     *          NOTE: myHand is a String and not String array because it is the answer returned from evaluateMyCards()
     * author: Demetrius Chatterjee
     */
    public String shouldBetForBet1(int minimumBet, int myChips, String[] allOtherPlayerHighestCards, String myHand){
        int betToMake = 0;
        if(myChips < minimumBet || myChips == 0){
            return "f" + betToMake;
        }else{
            int highestCardForOthers = HighestCard(allOtherPlayerHighestCards);
            int myHighestCard = Integer.parseInt(myHand.substring(1, myHand.length()));
            int howManyOfAKind = Integer.parseInt(myHand.substring(0, 1));
            if(howManyOfAKind == 2){
                if(myHighestCard<=highestCardForOthers){
                    betToMake = minimumBet+3;
                }else{
                    betToMake = minimumBet;
                }
            }else{
                if(myHighestCard<=highestCardForOthers){
                    betToMake = minimumBet+3;
                }else{
                    if(myHighestCard<=8 && minimumBet <= 10){
                        betToMake = minimumBet;
                    }else{
                        return "f" + betToMake;
                    }
                }
            }
        }
        if(betToMake > myChips){
            betToMake = minimumBet;
            if(betToMake > myChips){
                betToMake = 0;
                return "f" + betToMake;
            }
        }
        return "t" + betToMake;
    }
    /*
     * purpose: returns a 't' followed by the bet to make otherwise 'f' followed by 0, computes whether to make a bet for bet2
     *          NOTE: myHand is a String and not String array because it is the answer returned from evaluateMyCards()
     * author: Demetrius Chatterjee
     */
    public String shouldBetForBet2(int minimumBet, int myChips, String[] allOtherPlayersEvaluatedCards, String myHand){
        int betToMake = 0;
        if(myChips < minimumBet || myChips == 0){
            return "f" + betToMake;
        }else{
            int myHighestCard = Integer.parseInt(myHand.substring(1, myHand.length()));
            int howManyOfAKind = Integer.parseInt(myHand.substring(0, 1))-1;
            int highestOfAKindIndex = -1;
            for(int i=0;i<allOtherPlayersEvaluatedCards.length;i++){
                if(allOtherPlayersEvaluatedCards[i].charAt(0) == howManyOfAKind){
                    highestOfAKindIndex++;
                }
            }
            if(highestOfAKindIndex == -1){
                if(myChips > (minimumBet + 10)){
                    betToMake = minimumBet+10;
                }else{
                    betToMake = minimumBet+5;
                }
            }else{
                int highestKind = howManyOfAKind;
                int highestKindCard = Integer.parseInt(allOtherPlayersEvaluatedCards[0].substring(1, allOtherPlayersEvaluatedCards[0].length()));
                for(int j = 0;j<allOtherPlayersEvaluatedCards.length;j++){
                    if(allOtherPlayersEvaluatedCards[j].charAt(0) == highestKind && (Integer.parseInt(allOtherPlayersEvaluatedCards[j].substring(1, allOtherPlayersEvaluatedCards[j].length())) < highestKindCard)){
                        highestKindCard = Integer.parseInt(allOtherPlayersEvaluatedCards[j].substring(1, allOtherPlayersEvaluatedCards[j].length()));
                    }
                }
                if(myHighestCard < highestKind){
                    if(myChips > (minimumBet + 10)){
                        betToMake = minimumBet+10;
                    }else{
                        betToMake = minimumBet+5;
                    } 
                }else if(myHighestCard == highestKind){
                    if(myChips > (minimumBet + 10)){
                        betToMake = minimumBet+10;
                    }else{
                        betToMake = minimumBet+5;
                    }
                }else{
                    return "f" + betToMake;
                }
            }
            if(betToMake > myChips){
                betToMake = minimumBet;
                if(betToMake > myChips){
                    return "f" + betToMake;
                }
            }
            return "t" + betToMake;
        }
    }

    /*
     * purpose: returns the HighestCard in an ascending order (so the highest card is the smallest one as that is how I structured my CardOrder array at the start of Card class)
     *          the order of importance of each card is determined by encoding the card when it matches the element in CardOrder array and the index at that location of CardOrder is assigned
     *          this makes it easier to do comparisions throughout the program
     * author: Demetrius Chatterjee
     */
    public int HighestCard(String[] allCards){
        int cardRankingCounter = 0;
        int[] cardRankings = new int[allCards.length];
        for(int i = 0;i<allCards.length;i++){
            if(allCards[0].equals(cardOrder[i])){
                cardRankings[cardRankingCounter] = i;
                cardRankingCounter++;
                continue;
            }
        }
        Arrays.sort(cardRankings); 
        return cardRankings[0];
    }

    /*
     * purpose: returns the HighestCard out of the two in an ascending order (so the highest card is the smallest one as that is how I structured my CardOrder array at the start of Card class)
     *          the order of importance of each card is determined by encoding the card when it matches the element in CardOrder array and the index at that location of CardOrder is assigned
     *          this makes it easier to do comparisions throughout the program
     * author: Demetrius Chatterjee
     */
    public String HighestCard(String card1, String card2){
        int cardNum = 0;
        int highestIndex = 0;

        if(card1.charAt(0) == '1'){
            card1 = "10";
        }else{
            card1 = "";
            card1 += card1.charAt(0);
        }

        if(card2.charAt(0) == '1'){
            card2 = "10";
        }else{
            card2 = "";
            card2 += card2.charAt(0);
        }

        for(int i = 0;i<cardOrder.length;i++){
            if(card1.equals(cardOrder[i])){
                cardNum = 1;
                highestIndex = i;
                break;
            }else if(card2.equals(cardOrder[i])){
                cardNum = 2;
                highestIndex = i;
                break;
            }
        }
        return cardNum + "" + highestIndex;
    }
}