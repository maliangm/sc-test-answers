import java.math.BigDecimal;
import java.util.*;

public class FxRateCalculator {

    private static final Set<String> CURRENCY_CODES = new HashSet<>();

    CurrencyGraph currencyGraph = new CurrencyGraph();

    public FxRateCalculator() {
        initCurrencyData();
    }

    /**
     * Initialize the currency rates data.
     */
    private void initCurrencyData() {
        CURRENCY_CODES.add("EUR");
        CURRENCY_CODES.add("USD");
        CURRENCY_CODES.add("GBP");
        CURRENCY_CODES.add("AUD");
        CURRENCY_CODES.add("JPY");
        CURRENCY_CODES.add("INR");

        for (String currencyCode : CURRENCY_CODES) {
            currencyGraph.addCurrency(currencyCode);
        }

        currencyGraph.addRate("EUR", "USD", 1.2);
        currencyGraph.addRate("USD", "EUR", 1.0 / 1.2);

        currencyGraph.addRate("USD", "GBP", 0.75);
        currencyGraph.addRate("GBP", "USD", 1.0 / 0.75);

        currencyGraph.addRate("GBP", "AUD", 1.7);
        currencyGraph.addRate("AUD", "GBP", 1.0 / 1.7);

        currencyGraph.addRate("AUD", "JPY", 90.0);
        currencyGraph.addRate("JPY", "AUD", 1.0 / 90.0);

        currencyGraph.addRate("GBP", "JPY", 150);
        currencyGraph.addRate("JPY", "GBP", 1.0 / 150);

        currencyGraph.addRate("JPY", "INR", 0.6);
        currencyGraph.addRate("INR", "JPY", 1.0 / 0.6);
    }

    /**
     * Validate the input currency pair and invoke calculation.
     * @param currencyPair The input currency pair.
     * @return The FX rate
     */
    public double calculateRate(String currencyPair) {
        // Validate input
        if (currencyPair == null) {
            throw new RuntimeException("Invalid input: Invalid currency pair format. Should be in the format of 'FROM_CURRENCY/TO_CURRENCY', e.g. 'EUR/USD'");
        }

        String[] inputCurrencyCodes = currencyPair.split("/");

        if (inputCurrencyCodes.length != 2) {
            throw new RuntimeException("Invalid input: Invalid currency pair format. Should be in the format of 'FROM_CURRENCY/TO_CURRENCY', e.g. 'EUR/USD'");
        }

        String srcCurrencyCode = inputCurrencyCodes[0].toUpperCase();
        String destCurrencyCode = inputCurrencyCodes[1].toUpperCase();

        if (!CURRENCY_CODES.contains(srcCurrencyCode)) {
            throw new RuntimeException(String.format("Invalid input: '%s' is not a supported currency.", srcCurrencyCode));
        }

        if (!CURRENCY_CODES.contains(destCurrencyCode)) {
            throw new RuntimeException(String.format("Invalid input: '%s' is not a supported currency.", destCurrencyCode));

        }

        // Invoke the calculation
        return doCalculate(srcCurrencyCode, destCurrencyCode);
    }


    /**
     * Do the actual calculation
     * @param srcCurrencyCode Source currency code.
     * @param destCurrencyCode Destination currency code.
     * @return The FX rate.
     */
    private double doCalculate(String srcCurrencyCode, String destCurrencyCode) {

        final List<Double> rates = currencyGraph.searchRates(srcCurrencyCode, destCurrencyCode);

        if (rates == null) {
            throw new RuntimeException("Rate not found.");
        }

        double resultRate = 1.0;

        for (Double rate : rates) {
            resultRate *= rate;
        }

        return new BigDecimal(resultRate).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }


    public static void main(String[] args) {
        FxRateCalculator calculator = new FxRateCalculator();
        try {
            System.out.println("Please input a currency pair (e.g. EUR/USD) and press ENTER: ");
            Scanner sc = new Scanner(System.in);
            String currencyPair = sc.nextLine();
            double rate = calculator.calculateRate(currencyPair);
            System.out.println(String.format("The rate of %s is: %s", currencyPair, rate));
        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
        }
    }
}


/**
 * A simple graph used to find the exchange path.
 */
class CurrencyGraph {

    private Set<String> currencyVertices = new HashSet<>();
    private Map<String, Map<String, Double>> rateEdges = new HashMap<>();


    /**
     * Add currency code as vertex
     * @param currencyCode The currency code to be added.
     */
    public void addCurrency(String currencyCode) {
        currencyVertices.add(currencyCode);
    }

    /**
     * Add rates between currencies as edge
     * @param srcCurrency The source currency
     * @param destCurrency The destination currency
     * @param rate The FX rate
     */
    public void addRate(String srcCurrency, String destCurrency, double rate) {
        if (!currencyVertices.contains(srcCurrency)) {
            return;
        }
        if (!currencyVertices.contains(destCurrency)) {
            return;
        }

        Map<String, Double> rateMap = rateEdges.get(srcCurrency);
        if (rateMap == null) {
            rateMap = new HashMap<>();
            rateEdges.put(srcCurrency, rateMap);
        }
        rateMap.put(destCurrency, rate);
    }


    /**
     * Helper class for the search
     */
    private static class SearchNode {
        private String currencyCode;
        private double rate;
        private SearchNode parent;

        public SearchNode(String currencyCode, double rate, SearchNode parent) {
            this.currencyCode = currencyCode;
            this.rate = rate;
            this.parent = parent;
        }
    }


    /**
     * Searching the rates on the exchange path with BFS
     * @param srcCurrency The source currency.
     * @param destCurrency The destination currency.
     * @return List of FX rates on the path.
     */
    public List<Double> searchRates(String srcCurrency, String destCurrency) {

        Queue<SearchNode> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();

        queue.offer(new SearchNode(srcCurrency, 1.0, null));
        visited.add(srcCurrency);

        SearchNode resultNode = null;

        while(!queue.isEmpty()) {
            SearchNode currentNode = queue.poll();
            String currencyCode = currentNode.currencyCode;

            if (currencyCode.equals(destCurrency)) {
                resultNode = currentNode;
                break;
            }

            Map<String, Double> outEdges = rateEdges.get(currencyCode);

            for (Map.Entry<String, Double> edge : outEdges.entrySet()) {

                String toVertex = edge.getKey();
                if (visited.contains(toVertex)) {
                    continue;
                }

                Double rate = edge.getValue();

                queue.offer(new SearchNode(toVertex, rate, currentNode));
                visited.add(toVertex);
            }
        }

        List<Double> resultRates = null;

        if (resultNode != null) {
            resultRates = new ArrayList<>();
            while (resultNode != null) {
                resultRates.add(resultNode.rate);
                resultNode = resultNode.parent;
            }
        }

        return resultRates;
    }

}
