package com.walkingbuddy.app;
import org.junit.Test;
import static org.junit.Assert.*;
import java.time.LocalDate;

public class GameStateTest {
    private final LocalDate day=LocalDate.of(2026,10,4);
    private final long now=1791133200000L;
    private GameState newBuddy(){GameState s=new GameState(day,now);s.adopt(0,"Milo",now);return s;}
    @Test public void firstSensorReadingIsABaseline(){GameState s=newBuddy();s.sensor(12345,2,day);assertEquals(0,s.steps());s.sensor(12445,2,day);assertEquals(100,s.steps());}
    @Test public void persistedSensorContinuesWithoutDoubleCounting(){GameState s=newBuddy();s.sensor(1000,2,day);s.sensor(1500,2,day);s.sensor(1500,2,day);assertEquals(500,s.steps());assertEquals(5,s.leaves);}
    @Test public void rebootUsesANewBaselineEvenWhenCounterIsHigher(){GameState s=newBuddy();s.sensor(100,1,day);s.sensor(500,2,day);assertEquals(0,s.steps());s.sensor(600,2,day);assertEquals(100,s.steps());}
    @Test public void counterResetNeverMakesNegativeSteps(){GameState s=newBuddy();s.sensor(1000,1,day);s.sensor(20,1,day);assertEquals(0,s.steps());s.sensor(30,1,day);assertEquals(10,s.steps());}
    @Test public void newDayResetsTotalsAndRebaselinesSensor(){GameState s=newBuddy();s.sensor(1000,1,day);s.sensor(2000,1,day);s.advance(day.plusDays(1),now+86400000);s.sensor(2500,1,day.plusDays(1));assertEquals(0,s.steps());s.sensor(2600,1,day.plusDays(1));assertEquals(100,s.steps());assertEquals(1000,s.history.get(day).steps());}
    @Test public void delayedOldDaySensorDoesNotCreditToday(){GameState s=newBuddy();s.sensor(1000,1,day);s.advance(day.plusDays(1),now+86400000);s.sensor(1200,1,day);assertEquals(0,s.steps());}
    @Test public void manualAndSensorContributionsCombine(){GameState s=newBuddy();s.sensor(100,1,day);s.sensor(600,1,day);s.setManual(1000);assertEquals(1500,s.steps());assertEquals(500,s.sensorSteps);assertEquals(1000,s.manualSteps);}
    @Test public void manualEditIsAReplacement(){GameState s=newBuddy();s.setManual(1000);s.setManual(1200);assertEquals(1200,s.steps());}
    @Test public void editingStepsCannotRepeatRewards(){GameState s=newBuddy();s.setManual(6000);int leaves=s.leaves;int health=s.health;s.setManual(0);s.setManual(6000);assertEquals(leaves,s.leaves);assertEquals(health,s.health);}
    @Test public void reachingGoalRestoresHealthOnce(){GameState s=newBuddy();s.health=40;s.setManual(6000);assertEquals(55,s.health);s.setManual(8000);assertEquals(55,s.health);assertEquals(23,s.leaves);}
    @Test public void missedDaysLoseHealthEvenIfAppWasClosed(){GameState s=newBuddy();s.advance(day.plusDays(3),now+3*86400000);assertEquals(20,s.health);assertEquals(0,s.streak());}
    @Test public void successfulDayDoesNotLoseHealth(){GameState s=newBuddy();s.setManual(6000);s.advance(day.plusDays(1),now+86400000);assertEquals(95,s.health);assertEquals(1,s.streak());}
    @Test public void unfinishedTodayDoesNotBreakYesterdayStreak(){GameState s=newBuddy();s.setManual(6000);s.advance(day.plusDays(1),now+86400000);assertEquals(1,s.streak());s.setManual(6000);assertEquals(2,s.streak());assertEquals(2,s.bestStreak());}
    @Test public void skippedDayBreaksStreak(){GameState s=newBuddy();s.setManual(6000);s.advance(day.plusDays(2),now+2*86400000);s.setManual(6000);assertEquals(1,s.streak());assertEquals(1,s.bestStreak());}
    @Test public void goalChangesDoNotRewriteHistory(){GameState s=newBuddy();s.setManual(6000);s.advance(day.plusDays(1),now+86400000);s.setGoal(10000);assertEquals(6000,s.history.get(day).goal);assertTrue(s.history.get(day).hit());}
    @Test public void reversingClockCannotDuplicateDays(){GameState s=newBuddy();s.setManual(6000);s.advance(day.minusDays(1),now-86400000);assertEquals(day,s.day);assertTrue(s.history.isEmpty());assertEquals(6000,s.steps());}
    @Test public void careDecaysOnElapsedWholeHours(){GameState s=newBuddy();s.advance(day,now+2*3600000);assertEquals(69,s.food);assertEquals(67,s.water);assertEquals(81,s.clean);s.advance(day,now+2*3600000);assertEquals(69,s.food);}
    @Test public void feedSpendsALeafAndHasACooldown(){GameState s=newBuddy();s.food=10;s.care("feed",now);assertEquals(3,s.leaves);assertEquals(40,s.food);s.care("feed",now+1000);assertEquals(3,s.leaves);assertEquals(40,s.food);}
    @Test public void waterAndCleaningAreFree(){GameState s=newBuddy();s.water=0;s.clean=0;s.care("water",now);s.care("clean",now);assertEquals(4,s.leaves);assertEquals(35,s.water);assertEquals(30,s.clean);}
    @Test public void noFoodWithoutLeaves(){GameState s=newBuddy();s.leaves=0;s.food=5;s.care("feed",now);assertEquals(5,s.food);}
    @Test public void zeroHealthRequiresNewAdoption(){GameState s=newBuddy();s.advance(day.plusDays(5),now+5*86400000);assertEquals(0,s.health);s.setManual(6000);assertEquals(0,s.health);try{s.choose(1);fail();}catch(IllegalArgumentException expected){}s.adopted=false;s.adopt(1,"Maple",now);assertEquals(80,s.health);assertEquals(1,s.memories.size());}
    @Test public void companionsUnlockFromLifetimeSteps(){GameState s=newBuddy();assertFalse(s.unlocked(4));s.setManual(20000);assertTrue(s.unlocked(4));assertFalse(s.unlocked(5));s.advance(day.plusDays(1),now+86400000);assertTrue(s.unlocked(4));}
    @Test public void switchingCompanionsPreservesCareAndHealth(){GameState s=newBuddy();s.health=30;s.food=10;s.choose(2);assertEquals(30,s.health);assertEquals(10,s.food);assertEquals("Rusty",s.name);}
    @Test(expected=IllegalArgumentException.class) public void rejectsNegativeSteps(){newBuddy().setManual(-1);}
    @Test(expected=IllegalArgumentException.class) public void rejectsUnreasonableSteps(){newBuddy().setManual(200001);}
    @Test(expected=IllegalArgumentException.class) public void rejectsTinyGoal(){newBuddy().setGoal(100);}
    @Test(expected=IllegalArgumentException.class) public void rejectsLockedCompanion(){newBuddy().choose(7);}
}
