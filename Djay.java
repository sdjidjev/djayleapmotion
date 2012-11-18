import java.awt.*;
import java.awt.event.*;

class DjayListener extends Listener {
    final static int BOTTOM = 1, TOPLEFT = 2, TOPRIGHT = 3, NOTHING = 0;
    final static int SWIPELEFT = 1, SWIPERIGHT = 2, SCROLL = 3, SWIPEUP = 4, SCRATCH = 5;
    final static int LEFT=0, RIGHT=1, UP=2;
    final static int DOWN = 1, FLATISH = 3;
    final int FRAMETHRESHOLD=20;
    int area = NOTHING; // area detects whether a hand is in BOTTOM, TOPLEFT, or TOPRIGHT;
    int pointDirection = NOTHING;
    int event = NOTHING;
    int amount = 0;
    int delayAmount = 100; //shortest period between two of the same gesture
    int scrollDelayAmount = 2;
    int scratchDelayAmount = 2;
    int actionCounter = 0;
    int scrollCounter = 0;
    int scratchCounter = 0;
    boolean present = true;

    public void onInit(Controller controller) {
	System.out.println("Initialized");
    }

    public void onConnect(Controller controller) {
	System.out.println("Connected");
    }

    public void onDisconnect(Controller controller) {
	System.out.println("Disconnected");
    }
    
    int getArea(Vector pos) {
	if (pos!=null){
	    if (pos.getX()<0) {
		return LEFT;
	    } else {
		return RIGHT;
	    }
	}
	return NOTHING;
    }

    int getPointDirection(Finger finger) {
	int yDirection = (int)(Math.atan2(finger.tip().getDirection().getZ(), finger.tip().getDirection().getY()) * 180/Math.PI + 180);
	if (yDirection > 180) yDirection -= 360;
	    if (yDirection > -20) {
		return UP;
	    }
	    else if (yDirection < -120) {
		return DOWN;
	    }
	    else {
		return FLATISH;
	    }
    }

    int[] getEvent(FingerArray fingers, Controller controller) {
	int[] eventAr={NOTHING,NOTHING};
	    if (pointDirection == UP) {
		eventAr[0]=SCROLL;
		eventAr[1]=(int)(fingers.get(0).velocity().getX())/-100;	    
	    } else if (testSwipeLRU(controller,LEFT)){
		eventAr[0]=SWIPELEFT;
		eventAr[1]=0;
	    } else if (testSwipeLRU(controller,RIGHT)) {
		eventAr[0]=SWIPERIGHT;
		eventAr[1]=0;
	    } else if (testSwipeLRU(controller,UP)) {
		eventAr[0]=SWIPEUP;
		eventAr[1]=0;
	    } else if (pointDirection == DOWN){
		eventAr[0]=SCRATCH;
		eventAr[1]=(int)(fingers.get(0).velocity().getZ()/50);
	    }
	    return eventAr;
    }

    public Boolean testSwipeLRU(Controller controller,int space){
	Frame tempframe;
	HandArray temphands;
	long tempnumHands;
	Frame mainframe = controller.frame();
	HandArray hands = mainframe.hands();
	long numHands = hands.size();  
	for (int i=0;i<numHands;i++){
	    Boolean swipe[] = new Boolean[FRAMETHRESHOLD];
	    for (int k=0;k<FRAMETHRESHOLD;k++){
		tempframe = controller.frame(k);
		temphands = tempframe.hands();
		tempnumHands = temphands.size();
		if (tempnumHands>=numHands || tempnumHands>i){
		    Hand hand=hands.get(i);
		    FingerArray fingers = hand.fingers();
		    long numFingers= fingers.size();
		    Boolean validfinger=false;
		    if (numFingers>=1){
			for (int j=0;j < numFingers;j++){
			    Finger finger = fingers.get(j);
			    if (space==0){
				if (finger.velocity().getX()>700 && Math.abs(finger.velocity().getY())<250 && Math.abs(finger.velocity().getZ())<250){
				    validfinger=true;
				}
			    } else if (space==1){
				if (finger.velocity().getX()<-700 && Math.abs(finger.velocity().getY())<250 && Math.abs(finger.velocity().getZ())<250){
				    validfinger=true;
				}
			    } else {
				if (finger.velocity().getY()>1000){
				    validfinger=true;
				}
			    }	     
			}
		    }
		    swipe[k]=validfinger;
		} else {
		    swipe[k]=false;
		}
	    }
	    Double total=0.0;
	    for (int k=0;k<FRAMETHRESHOLD;k++){
		if (swipe[k]==true){
		    total++;
		}
	    }
	    if ((total/FRAMETHRESHOLD)>.85){
		return true;
	    }
	}
   
	return false;
    }

    Vector getPos(FingerArray fingers, long numFingers){
	Vector pos = new Vector(0, 0, 0);
	for (int i = 0; i < numFingers; ++i) {
	    Finger finger = fingers.get(i);
	    Ray tip = finger.tip();
	    pos.setX(pos.getX() + tip.getPosition().getX());
	    pos.setY(pos.getY() + tip.getPosition().getY());
	    pos.setZ(pos.getZ() + tip.getPosition().getZ());
	}
	pos = new Vector(pos.getX()/numFingers, pos.getY()/numFingers, pos.getZ()/numFingers);
	return pos;
    }


    public void onFrame(Controller controller) {
	// update counters
	if (scrollCounter > 0) {
	    scrollCounter--;
	}
	if (scratchCounter > 0) {
	    scratchCounter--;
	}
	if (actionCounter > 0) {
	    actionCounter--;
	}

	Frame frame = controller.frame();
	HandArray hands = frame.hands();
	long numHands = hands.size();
	event=NOTHING;
	area = NOTHING;
	pointDirection = NOTHING;
	present = false;
	if (numHands >= 1) {
	    present = true;
	    // Get the first hand
	    Hand hand = hands.get(0);
	    // Check if the hand has any fingers
	    FingerArray fingers = hand.fingers();
	    long numFingers = fingers.size();
	    Vector pos=null;
	    if (numFingers >= 1) {
		pos=getPos(fingers,numFingers);
		area = getArea(pos);
		pointDirection = getPointDirection(fingers.get(0));
	    } else {
		// Check if the hand has a palm
		Ray palmRay = hand.palm();
		if (palmRay != null) {
		    Vector palm = palmRay.getPosition();
		    //fingers not detected, get area of your hand
		    Vector palmPos = new Vector(palm.getX(), palm.getY(), palm.getZ());
		    area = getArea(palmPos);
		}
	    }
	    int[] eventAr=getEvent(fingers, controller);
	    event=eventAr[0];
	    amount=eventAr[1];
	}

	    // key presses
	    if (present) {
		try {
		Robot robot = new Robot();
		if (pointDirection == DjayListener.UP) {
		    if (event == DjayListener.SCROLL) {
			if (scrollCounter == 0) {
			    robot.mouseMove(500,700);
			    robot.mouseWheel(amount);
			    scrollCounter = scrollDelayAmount;
			}
		    }
		}
		else if (area == DjayListener.LEFT) {
		    switch (event) {
		    case DjayListener.SWIPEUP: {
			if (actionCounter == 0) {
			    robot.keyPress(KeyEvent.VK_1);
			    robot.keyRelease(KeyEvent.VK_1);
			    actionCounter = delayAmount;
			}
			break;
		    }
		    case DjayListener.SWIPERIGHT: {
			if (actionCounter == 0) {
			    robot.keyPress(KeyEvent.VK_META); //load
			    robot.keyPress(KeyEvent.VK_LEFT);
			    robot.keyRelease(KeyEvent.VK_LEFT);
			    robot.keyRelease(KeyEvent.VK_META);
			    robot.keyPress(KeyEvent.VK_2); //sync
			    robot.keyRelease(KeyEvent.VK_2);
			    robot.keyPress(KeyEvent.VK_DOWN); //next song
			    robot.keyRelease(KeyEvent.VK_DOWN);
			    actionCounter = delayAmount;
			}
			break;
		    }
		    case DjayListener.SWIPELEFT: {
			if (actionCounter == 0) {
			    robot.keyPress(KeyEvent.VK_META); //load
			    robot.keyPress(KeyEvent.VK_RIGHT);
			    robot.keyRelease(KeyEvent.VK_RIGHT);
			    robot.keyRelease(KeyEvent.VK_META);
			    robot.keyPress(KeyEvent.VK_9); //sync
			    robot.keyRelease(KeyEvent.VK_9);
			    robot.keyPress(KeyEvent.VK_DOWN); //next song
			    robot.keyRelease(KeyEvent.VK_DOWN);
			    actionCounter = delayAmount;
			}
			break;
		    }
		    case DjayListener.SCRATCH: {
			if (scratchCounter == 0) {
			    robot.mouseMove(350,365);
			    robot.mouseWheel(amount);
			    scratchCounter = scratchDelayAmount;
			}
			break;
		    }
		    }
		}
		else if (area == DjayListener.RIGHT) {
		    switch (event) {
		    case DjayListener.SWIPEUP: {
			if (actionCounter == 0) {
			    robot.keyPress(KeyEvent.VK_0);
			    robot.keyRelease(KeyEvent.VK_0);
			    actionCounter = delayAmount;
			}
			break;
		    }
		    case DjayListener.SWIPERIGHT: {
			if (actionCounter == 0) {
			    robot.keyPress(KeyEvent.VK_META); //load
			    robot.keyPress(KeyEvent.VK_LEFT);
			    robot.keyRelease(KeyEvent.VK_LEFT);
			    robot.keyRelease(KeyEvent.VK_META);
			    robot.keyPress(KeyEvent.VK_2); //sync
			    robot.keyRelease(KeyEvent.VK_2);
			    robot.keyPress(KeyEvent.VK_DOWN); //next song
			    robot.keyRelease(KeyEvent.VK_DOWN);
			    actionCounter = delayAmount;
			}
			break;
		    }
		    case DjayListener.SWIPELEFT: {
			if (actionCounter == 0) {
			    robot.keyPress(KeyEvent.VK_META); //load
			    robot.keyPress(KeyEvent.VK_RIGHT);
			    robot.keyRelease(KeyEvent.VK_RIGHT);
			    robot.keyRelease(KeyEvent.VK_META);
			    robot.keyPress(KeyEvent.VK_9); //sync
			    robot.keyRelease(KeyEvent.VK_9);
			    robot.keyPress(KeyEvent.VK_DOWN); //next song
			    robot.keyRelease(KeyEvent.VK_DOWN);
			    actionCounter = delayAmount;
			}
			break;
		    }
		    case DjayListener.SCRATCH: {
			if (scratchCounter == 0) {
			    robot.mouseMove(600, 365);
			    robot.mouseWheel(amount);
			    scratchCounter = scratchDelayAmount;
			}
			break;
		    }
		    }
		}
		} catch (AWTException e) { 
		    e.printStackTrace();
		}
	    }
    }
}


class Djay {

    public static void main(String[] args) {
	// Create a sample listener and assign it to a controller to receive events
	DjayListener listener = new DjayListener();
	Controller controller = new Controller(listener);

	// Keep this process running until Enter is pressed
	System.out.println("Press Enter to quit...");

	System.console().readLine();

	// The controller must be disposed of before the listener
	controller = null;
    }
}
