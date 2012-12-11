package test;

/**
 * Created with IntelliJ IDEA.
 * User: pgouvas
 * Date: 12/8/12
 * Time: 12:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class RegexTester {

    private static final String INITIATE_PREFIX    = "RedPhone call:";
    private static final String GV_INITIATE_PREFIX = "^\\+[0-9]+ \\- " + INITIATE_PREFIX + ".+";

    private static final String VERIFY_PREFIX      = "A RedPhone is trying to verify you:";
    private static final String GV_VERIFY_PREFIX   = "^\\+[0-9]+ \\- " + VERIFY_PREFIX + ".+";

    private static String  msg1 = "306949193033 - A RedPhone is trying to verify you:1apEkRs95u5dv2vxVwMb1Sti";
    private static String  msg2 = "A RedPhone is trying to verify you:+306949193033";

    private static String message = msg2;
    public static void main(String[] args) {
        RegexTester.testRegex();
    }

    public static void testRegex(){
        System.out.println( !message.startsWith(VERIFY_PREFIX) );
        System.out.println( !message.matches(GV_VERIFY_PREFIX) );

        if (!message.startsWith(VERIFY_PREFIX) && !message.matches(GV_VERIFY_PREFIX)) {
            System.out.println( "Not a verifier challenge...");
        } else {
            System.out.println("It is a verification message");
        }
    }//testRegex
}
