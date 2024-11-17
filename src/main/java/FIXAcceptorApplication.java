import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.ExecutionReport;
import quickfix.fix44.NewOrderSingle;
import quickfix.fix44.MessageFactory;


public class FIXAcceptorApplication extends MessageCracker implements Application {

    @Override
    public void onCreate(SessionID sessionID) {
        System.out.println("Session created: " + sessionID);
    }

    @Override
    public void onLogon(SessionID sessionID) {
        System.out.println("Client logged on: " + sessionID);
    }

    @Override
    public void onLogout(SessionID sessionID) {
        System.out.println("Client logged out: " + sessionID);
    }

    @Override
    public void toAdmin(Message message, SessionID sessionID) {
        System.out.println("Outgoing admin message: " + message);
    }

    @Override
    public void fromAdmin(Message message, SessionID sessionID)
            throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
        System.out.println("Incoming admin message: " + message);
    }

    @Override
    public void toApp(Message message, SessionID sessionID) throws DoNotSend {
        System.out.println("Outgoing application message: " + message);
    }

    @Override
    public void fromApp(Message message, SessionID sessionID)
            throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        System.out.println("Incoming application message: " + message);

        // Process the application message
        try {
            crack(message, sessionID);
        } catch (UnsupportedMessageType | FieldNotFound | IncorrectTagValue e) {
            e.printStackTrace();
        }
    }

    // Example: Handle a New Order Single (MsgType=D)
    public void onMessage(NewOrderSingle order, SessionID sessionID) throws FieldNotFound {
        System.out.println("Received New Order Single:");
        System.out.println("ClOrdID: " + order.getClOrdID().getValue());
        System.out.println("Symbol: " + order.getSymbol().getValue());
        System.out.println("Side: " + order.getSide().getValue());
        System.out.println("OrderQty: " + order.getOrderQty().getValue());
        System.out.println("Price: " + order.getPrice().getValue());

        ExecutionReport executionReport = new ExecutionReport(
                new OrderID("12345"),
                new ExecID("54321"),
                new ExecType(ExecType.NEW),
                new OrdStatus(OrdStatus.FILLED),
                order.getSide(),
                new LeavesQty(0),
                new CumQty(order.getOrderQty().getValue()),
                new AvgPx(order.getPrice().getValue())
        );

        try {
            Session.sendToTarget(executionReport, sessionID);
        } catch (SessionNotFound e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws ConfigError, InterruptedException {
        SessionSettings settings = new SessionSettings("config.cfg");

        FIXAcceptorApplication application = new FIXAcceptorApplication();

        MessageStoreFactory storeFactory = new FileStoreFactory(settings);
        LogFactory logFactory = new FileLogFactory(settings);
        MessageFactory messageFactory = new quickfix.fix44.MessageFactory(); // For FIX 4.4 messages
        SocketAcceptor acceptor = new SocketAcceptor(application, storeFactory, settings, logFactory, messageFactory);

        System.out.println("Starting FIX Acceptor...");
        acceptor.start();

        System.out.println("FIX Acceptor running. Press Ctrl+C to stop.");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            acceptor.stop();
        }));

        Thread.currentThread().join();
    }
}
