import java.awt.*;
import java.awt.event.*;

/* DJ with your hands in Djay using Leap Motion */

class DjayListener extends Listener {
    final static int NAH=0,LEFT=1, RIGHT=2, POSTHRESH=170, NEGTHRESH=-170, BASELINE=100;
    int delayAmount = 250; //shortest period between two of the same gesture
    int scrollDelayAmount = 2;
    int scratchDelayAmount = 15;
    int playCounterL = 0;
    int playCounterR = 0;
    int scrollCounter = 0;
    int scratchCounterR = 0;
    int scratchCounterL = 0;
    int swipeCounterL = 0;
    int swipeCounterR = 0;

    public void onInit(Controller controller) {
	System.out.println("Initialized");
    }

    public void onConnect(Controller controller) {
	System.out.println("Connected");
    }

    public void onDisconnect(Controller controller) {
	System.out.println("Disconnected");
    }
    
    public int mixerOne(Hand hand) {
	FingerArray fingers = hand.fingers();
	long numFingers = fingers.size();
	if(numFingers==2 && getPos(hand).getZ()>=40) {
	    Finger finger=fingers.get(0);
	    int amount = (int)(finger.velocity().getX())/BASELINE;
	    return amount;
	} else {
	    return 0;
	}
    }
    
    public int mixerTwo(Hand hand0, Hand hand1) {
	Hand left, right;
	Vector pos0 = getPos(hand0);
	Vector pos1 = getPos(hand1);
	Vector leftPos, rightPos;
	if (pos1.getX()<0) {
	    left=hand0;
	    leftPos=pos0;
	    right=hand1;
	    rightPos=pos1;
	} else {
	    left=hand1;
	    leftPos=pos1;
	    right=hand0;
	    rightPos=pos0;
	}
	double thresh = leftPos.getZ()-rightPos.getZ();
	if(thresh>POSTHRESH) {
	    return LEFT;
	} else if (thresh<NEGTHRESH) {
	    return RIGHT;
	} else {
	    return NAH;
	}
    }

    public Vector getPos(Hand hand){
	FingerArray fingers = hand.fingers();
	long numFingers = fingers.size();
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
    
    public boolean playPause(Hand hand) {
	Vector pos=getPos(hand);
	Ray palmRay = hand.palm();
	if (palmRay != null) {
	    Vector normal = hand.normal();
	    if (normal != null) {
		double pitchAngle = Math.atan2(normal.getZ(), normal.getY()) * 180/Math.PI + 180;
		if (pitchAngle > 180) pitchAngle -= 360;
		if (pitchAngle < -15) {
		    return true; 
		}
	    }
	}
	return false;
    }
    
    public boolean noFingers(Hand hand) {
    	return hand.fingers().size() < 2;
    }
    
    public int scratch(Hand hand) {
	FingerArray fingers = hand.fingers();
	long numFingers = fingers.size();
	if (numFingers != 2) {
	    return 0;
	}
	//get average finger velocity
	Vector vel = new Vector(0, 0, 0);
        for (int i = 0; i < numFingers; i++) {
          Finger finger = fingers.get(i);
          vel.setX(vel.getX() + finger.velocity().getX());
          vel.setZ(vel.getZ() + finger.velocity().getZ());
        }
        vel = new Vector(vel.getX()/numFingers, 0, vel.getZ()/numFingers);
	int euclidean = (int)(Math.sqrt(Math.pow(vel.getX(),2) + Math.pow(vel.getZ(),2)/100));
	return euclidean;
    }
    
    public Boolean rollRight(Hand hand){
	Vector normal = hand.normal();
	FingerArray fingers = hand.fingers();
	long numFingers=fingers.size();
	if (numFingers>=3){
	    if (normal != null) {
		// Calculate the hand's pitch, roll, and yaw angles
		double rollAngle = Math.atan2(normal.getX(), normal.getY()) * 180/Math.PI + 180;
		// Ensure the angles are between -180 and +180 degrees
		if (rollAngle > 180) rollAngle -= 360;
		if (rollAngle>=65){
		    return true;
		}
	    }
	}
	return false;
    }

    public Boolean rollLeft(Hand hand){
	Vector normal = hand.normal();
	FingerArray fingers = hand.fingers();
	long numFingers=fingers.size();
	if (numFingers>=3){
	    if (normal != null) {
		// Calculate the hand's pitch, roll, and yaw angles
		double rollAngle = Math.atan2(normal.getX(), normal.getY()) * 180/Math.PI + 180;
		// Ensure the angles are between -180 and +180 degrees
		if (rollAngle > 180) rollAngle -= 360;
		if (rollAngle<=-65){
		    return true;
		}
	    }
	}
	return false;
    }


    public void onFrame(Controller controller) {
	// update counters
	if (scrollCounter > 0) {
	    scrollCounter--;
	}
	if (scratchCounterR > 0) {
	    scratchCounterR--;
	}
	if (scratchCounterL > 0) {
	    scratchCounterL--;
	}
	if (swipeCounterL > 0) {
	    swipeCounterL--;
	}
	if (swipeCounterR > 0) {
	    swipeCounterR--;
	}
	if (playCounterR > 0) {
	    playCounterR--;
	}
	if (playCounterL > 0) {
	    playCounterL--;
	}
	Boolean swipeL=false; 
	Boolean swipeR=false; 
	Boolean playL=false;
	Boolean playR=false; 
	Boolean scratchL=false; 
	Boolean scratchR=false;
	Boolean endL=false; 
	Boolean endR=false; 
	Boolean mix1=false;
	int mamount=0;
	int samountL=0;
	int samountR=0;
	int mix=0;
	Frame frame = controller.frame();
	HandArray hands = frame.hands();
	long numHands = hands.size();
	if (numHands==1) {
	    Hand hand=hands.get(0);
	    Vector pos=getPos(hand);
	    mamount=mixerOne(hand);
	    if (mamount==0){
		mix1=false;
	    } else {
		mix1=true;
	    }
	    if (pos.getX()<0){
		swipeL=rollLeft(hand);
		playL=playPause(hand);
		samountL=scratch(hand);
		endL=noFingers(hand);
		if (samountL==0){
		    scratchL=false;
		} else {
		    scratchL=true;
		}
	    } else {
		swipeR=rollRight(hand);
		playR=playPause(hand);
		samountR=scratch(hand);
		endR=noFingers(hand);
		if (samountR==0){
		    scratchR=false;
		} else {
		    scratchR=true;
		}
	    }
	} else if (numHands==2){
	    Hand hand0=hands.get(0);
	    Hand hand1=hands.get(1);
	    Vector pos0=getPos(hand0);
	    Vector pos1=getPos(hand1);
	    if (pos0.getX()<0){
		swipeL=rollLeft(hand0);
		playL=playPause(hand0);
		samountL=scratch(hand0);
		endL=noFingers(hand0);
		if (samountL==0){
		    scratchL=false;
		} else {
		    scratchL=true;
		}
		swipeR=rollRight(hand1);
		playR=playPause(hand1);
		samountR=scratch(hand1);
		endR=noFingers(hand1);
		if (samountR==0){
		    scratchR=false;
		} else {
		    scratchR=true;
		}
	    } else {
		swipeL=rollLeft(hand1);		
		playL=playPause(hand1);
		samountL=scratch(hand1);
		endL=noFingers(hand1);
		if (samountL==0){
		    scratchL=false;
		} else {
		    scratchL=true;
		}
		swipeR=rollRight(hand0);
		playR=playPause(hand0);
		samountR=scratch(hand0);
		endR=noFingers(hand0);
		if (samountR==0){
		    scratchR=false;
		} else {
		    scratchR=true;
		}
	    }
	    mix=mixerTwo(hand0,hand1);
	} else {
	    //DO nothing, ain't no hands, or too many
	    mix=0;
	}
	
	// key presses
	try {
	    Robot robot = new Robot();
	    if (mix==1 || mix==2) {
		if (scrollCounter == 0) {
		    if (mix==1){
			robot.keyPress(KeyEvent.VK_LEFT);
			robot.keyRelease(KeyEvent.VK_LEFT);
			scrollCounter = scrollDelayAmount;
		    } else {
			robot.keyPress(KeyEvent.VK_RIGHT);
			robot.keyRelease(KeyEvent.VK_RIGHT);
			scrollCounter = scrollDelayAmount;
		    }   
		}
	    }
	    if (mix1 && scrollCounter == 0){
		robot.mouseMove(500,630);
		robot.mouseWheel(mamount);
		scrollCounter = scrollDelayAmount;
	    }
	    if (swipeL || swipeR) {
		if (swipeCounterL==0 && swipeL){
		    robot.keyPress(KeyEvent.VK_META); //load
		    robot.keyPress(KeyEvent.VK_LEFT);
		    robot.keyRelease(KeyEvent.VK_LEFT);
		    robot.keyRelease(KeyEvent.VK_META);
		    robot.keyPress(KeyEvent.VK_2); //sync
		    robot.keyRelease(KeyEvent.VK_2);
		    robot.keyPress(KeyEvent.VK_DOWN); //next song
		    robot.keyRelease(KeyEvent.VK_DOWN);
		    swipeCounterL = delayAmount;
		} 
		if (swipeCounterR==0 && swipeR){
		    robot.keyPress(KeyEvent.VK_META); //load
		    robot.keyPress(KeyEvent.VK_RIGHT);
		    robot.keyRelease(KeyEvent.VK_RIGHT);
		    robot.keyRelease(KeyEvent.VK_META);
		    robot.keyPress(KeyEvent.VK_9); //sync
		    robot.keyRelease(KeyEvent.VK_9);
		    robot.keyPress(KeyEvent.VK_DOWN); //next song
		    robot.keyRelease(KeyEvent.VK_DOWN);
		    swipeCounterR = delayAmount;
		}
	    }
	    if (scratchL || scratchR){
		if (scratchCounterL == 0 && scratchL) {
		    robot.mouseMove(350,365);
		    robot.mouseWheel(samountL);
		    scratchCounterL = scratchDelayAmount;
		} 
		if (scratchCounterR == 0 && scratchR) {
		    System.out.println("YO");
		    robot.mouseMove(600, 365);
		    robot.mouseWheel(samountR);
		    scratchCounterR = scratchDelayAmount;
		}
	    } 
	    if (playL || playR){
		if (playCounterL==0 && playL){
		    robot.keyPress(KeyEvent.VK_1);
		    robot.keyRelease(KeyEvent.VK_1);
		    playCounterL = delayAmount;
		}
		if (playCounterR==0 && playR) {
		    robot.keyPress(KeyEvent.VK_0);
		    robot.keyRelease(KeyEvent.VK_0);
		    playCounterR = delayAmount;
		}
	    }
	    //if (endL || endR){
	    //	robot.keyPress(KeyEvent.VK_Z);
	    //	robot.keyRelease(KeyEvent.VK_Z);
	    //}
	} catch (AWTException e) { 
	    e.printStackTrace();
		}
    }
}



class newDjay {

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
