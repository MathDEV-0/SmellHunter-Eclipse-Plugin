package com.plugin.view;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.List;
import java.util.stream.IntStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.internal.ole.win32.COMObject;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swtchart.Chart;
import org.eclipse.swtchart.IAxis;
import org.eclipse.swtchart.IAxisSet;
import org.eclipse.swtchart.IBarSeries;
import org.eclipse.swtchart.ISeries.SeriesType;
import org.eclipse.swtchart.Range;
import org.eclipse.ui.part.ViewPart;
import org.osgi.framework.Bundle;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
/**
 * View principal do plugin de detecção de "bad smells".
 * Apresenta UIs para gerenciamento de arquivos, visualização de métricas em tabela e gráfico,
 * e um histórico de execuções persistido em JSON.
 */
public class MyView extends ViewPart {
    public static final String ID = "com.plugin.view.view";

    // --- UI Components & State Management ---
    private Composite chartComposite;
    private Table metricsTable;
    private Table historyTable;
    private Button executeButton;

    // Armazena os caminhos dos arquivos: [0]=smelldsl, [1]=valores métricas, [2]=limites métricas
    private final String[] filePaths = new String[3];
    private Text[] fileNameTexts = new Text[3];
    private Canvas[] statusIndicators = new Canvas[3];


 // Armazena os dados das métricas e seus limites carregados dos arquivos
    private Map<String, Double> metricValues = new HashMap<>();
    private Map<String, double[]> metricThresholds = new HashMap<>();

    private static final String HISTORY_FILE_NAME = "execution_history.json";

    // Cores personalizadas para a UI
    private Color greenColor, redColor, yellowColor, primaryBlue, lightGrayBackground, fontColor;

    private final String[] metricas = {"LOC", "NOA", "NOM", "WMC", "LCOM", "CBO", "DIT", "NOC"};

    /**
     * Classe interna para representar uma entrada no log de histórico.
     * Utilizada para serialização/deserialização com Gson.
     */
    
    /**
     * Classe interna para representar uma entrada no log de histórico.
     * Utilizada para serialização/deserialização com Gson.
     */
    private static class ExecutionLogEntry {
        String timestamp;
        String script;
        String badSmells;
        String status;

        ExecutionLogEntry(String timestamp, String script, String badSmells, String status) {
            this.timestamp = timestamp;
            this.script = script;
            this.badSmells = badSmells;
            this.status = status;
        }
    }

    @Override
    public void createPartControl(Composite parent) {
        initializeColors(parent.getDisplay());
        parent.setLayout(new GridLayout(1, false));
        parent.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

        // 1. Cria a área de gerenciamento de arquivos na parte superior.
        createFileManagementArea(parent);
        createCredentialsArea(parent);

        // 2. Cria um container para agrupar a tabela de métricas e o gráfico lado a lado.
        Composite metricsAndChartComposite = new Composite(parent, SWT.NONE);
        metricsAndChartComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        metricsAndChartComposite.setLayout(new GridLayout(2, true));
        
        // 3. Adiciona a tabela e o gráfico dentro do container.
        createMetricsTable(metricsAndChartComposite);
        createChartArea(metricsAndChartComposite);

        // 4. Cria a área de histórico de execuções na parte inferior.
        createHistoryArea(parent);
        
        // Carrega o histórico de execuções do arquivo JSON ao iniciar a view.
        loadHistory();
    }

    /**
     * Inicializa as cores que serão utilizadas na UI.
     * @param display O display atual.
     */
    private void initializeColors(Display display) {
        greenColor = new Color(display, 76, 175, 80);   // Verde para LOW
        yellowColor = new Color(display, 255, 193, 7); // Amarelo para MEDIUM
        redColor = new Color(display, 244, 67, 54);     // Vermelho para HIGH

        fontColor = display.getSystemColor(SWT.COLOR_DARK_GRAY);
    }

    /**
     * Cria a seção de gerenciamento de arquivos com slots para upload e o botão de execução.
     * @param parent O composite pai.
     */
    private void createFileManagementArea(Composite parent) {
    	Group fileGroup = new Group(parent, SWT.NONE);
        fileGroup.setText("File Management");
        fileGroup.setLayout(new GridLayout(5, false));
        fileGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        fileGroup.setFont(new Font(parent.getDisplay(), "Segoe UI", 9, SWT.BOLD));
        
        GridLayout groupLayout = (GridLayout) fileGroup.getLayout();
        groupLayout.marginWidth = 10;
        groupLayout.marginHeight = 10;
        groupLayout.verticalSpacing = 8;

        createFileSlot(fileGroup, 0, "Specifications (*.smelldsl)", new String[]{"*.smelldsl"});
        createFileSlot(fileGroup, 1, "Metric Values (*.csv, *.json)", new String[]{"*.csv", "*.json"});
        createFileSlot(fileGroup, 2, "Metric Limits (*.csv, *.json)", new String[]{"*.csv", "*.json"});

        executeButton = new Button(fileGroup, SWT.PUSH);
        executeButton.setText("Detect Badsmells");
        GridData executeButtonData = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
        executeButtonData.horizontalSpan = 5;
        executeButtonData.verticalIndent = 10;
        executeButton.setLayoutData(executeButtonData);
        executeButton.setEnabled(false);
        executeButton.setFont(new Font(parent.getDisplay(), "Segoe UI", 9, SWT.NORMAL));

        executeButton.addListener(SWT.Selection, e -> executeDetectionProcess());
    }

    /**
     * Cria um slot individual para anexar um arquivo.
     * @param parent O composite pai.
     * @param index O índice do arquivo no array filePaths.
     * @param labelText O texto do label do slot.
     * @param extensions As extensões de arquivo permitidas.
     */
    private void createFileSlot(Composite parent, final int index, String labelText, String[] extensions) {
        Label label = new Label(parent, SWT.NONE);
        label.setText(labelText);
        label.setFont(new Font(parent.getDisplay(), "Segoe UI", 9, SWT.NORMAL));

        fileNameTexts[index] = new Text(parent, SWT.BORDER | SWT.READ_ONLY);
        fileNameTexts[index].setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        fileNameTexts[index].setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        fileNameTexts[index].setFont(new Font(parent.getDisplay(), "Consolas", 9, SWT.NORMAL));

        statusIndicators[index] = new Canvas(parent, SWT.NONE);
        GridData canvasData = new GridData(SWT.CENTER, SWT.CENTER, false, false);
        canvasData.widthHint = 16;
        canvasData.heightHint = 16;
        statusIndicators[index].setLayoutData(canvasData);
        statusIndicators[index].addPaintListener(e -> {
            Color color = filePaths[index] != null ? greenColor : redColor;
            e.gc.setBackground(color);
            e.gc.fillOval(0, 0, 15, 15);
        });

        Button attachButton = new Button(parent, SWT.PUSH);
        attachButton.setText("Attach...");
        attachButton.setFont(new Font(parent.getDisplay(), "Segoe UI", 9, SWT.NORMAL));
        attachButton.addListener(SWT.Selection, e -> {
            FileDialog dialog = new FileDialog(getSite().getShell(), SWT.OPEN);
            dialog.setFilterExtensions(extensions);
            String path = dialog.open();
            if (path != null) {
                filePaths[index] = path;
                fileNameTexts[index].setText(new File(path).getName());
                statusIndicators[index].redraw();
                checkExecuteButtonState();
            }
        });

        Button deleteButton = new Button(parent, SWT.PUSH);
        deleteButton.setText("Remove");
        deleteButton.setFont(new Font(parent.getDisplay(), "Segoe UI", 9, SWT.NORMAL));
        deleteButton.addListener(SWT.Selection, e -> {
            filePaths[index] = null;
            fileNameTexts[index].setText("");
            statusIndicators[index].redraw();
            checkExecuteButtonState();
        });
    }

    /**
     * Verifica se todos os arquivos foram anexados e habilita/desabilita o botão de execução.
     */
    private void checkExecuteButtonState() {
        boolean allFilesLoaded = Arrays.stream(filePaths).allMatch(p -> p != null);
        executeButton.setEnabled(allFilesLoaded);

    }
    
    
    private Text credentialsJsonText;
    
    /*Context information 
     * */
    private void createCredentialsArea(Composite parent) {

        Group credentialsGroup = new Group(parent, SWT.NONE);
        credentialsGroup.setText("Credenciais / Metadados (JSON)");
        credentialsGroup.setLayout(new GridLayout(1, false));
        credentialsGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        credentialsGroup.setFont(new Font(parent.getDisplay(), "Segoe UI", 9, SWT.BOLD));

        credentialsJsonText = new Text(
                credentialsGroup,
                SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL
        );

        GridData textData = new GridData(SWT.FILL, SWT.FILL, true, false);
        textData.heightHint = 100;
        credentialsJsonText.setLayoutData(textData);

        credentialsJsonText.setFont(new Font(parent.getDisplay(), "Consolas", 9, SWT.NORMAL));
        credentialsJsonText.setMessage("user_id is required");
        credentialsJsonText.setText(
            "{\n" +
    		"  \"id\": \"\",\n" +
            "  \"user_id\": \"\",\n" +
            "  \"org_id\": \"\",\n" +
            "  \"loc_id\": \"\",\n" +
            "  \"project_id\": \"\",\n" +
            "  \"file_path\": \"\",\n" +
            "  \"language\": \"java\",\n" +
            "  \"branch\": \"main\",\n" +
            "  \"commit_sha\": \"\"\n" +
            "}"
        );
    }

    /**
=======
    }
    
    /**
     * Ponto de entrada para o processo de detecção. É chamado quando o botão "Detectar" é clicado.
     */
    
    
    private void executeDetectionProcess() {
        if (!executeButton.isEnabled()) {
            MessageDialog.openError(getSite().getShell(), "Error", "All files must be loaded.");
            return;
        }
        
        executeButton.setText("Processing...");
        executeButton.setEnabled(false);
        String credentialsJson = credentialsJsonText.getText();
        
        new Thread(() -> {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            String scriptName = new File(filePaths[0]).getName();
            
            // These variables will be captured by the lambda for UI update
            final Map<String, Double>[] metricsLoadedRef = new Map[1];
            final Map<String, double[]>[] thresholdsLoadedRef = new Map[1];
            final SmellResult[] smellResultRef = new SmellResult[1];
            final Exception[] exceptionRef = new Exception[1];
            final String[] smellIdRef = new String[1];
            
            try {
                // 1) Load metrics and thresholds (OFF-UI THREAD - SAFE)
                thresholdsLoadedRef[0] = readMetricThresholds(filePaths[2]);
                metricsLoadedRef[0] = readMetricValues(filePaths[1]);
                
                
                System.out.println("[DEBUG] Metrics loaded: " + metricsLoadedRef[0].keySet());
                System.out.println("[DEBUG] Thresholds loaded: " + (thresholdsLoadedRef[0] != null ? thresholdsLoadedRef[0].keySet() : "null"));
                System.out.println("[DEBUG] Thresholds size: " + (thresholdsLoadedRef[0] != null ? thresholdsLoadedRef[0].size() : 0));
                String dslCode = new String(
                    Files.readAllBytes(Paths.get(filePaths[0])),
                    StandardCharsets.UTF_8
                );
                
                // 2) Parse credentials JSON
                Gson gson = new Gson();
                Type type = new TypeToken<Map<String, Object>>(){}.getType();
                Map<String, Object> metadata = gson.fromJson(credentialsJson, type);
                
                // 3) Call analyze API
                String analyzeUrl = "http://localhost:5000/analyze";
                AnalyzeResponse analyzeResp = callAnalyzeApi(
                    analyzeUrl,
                    dslCode,
                    metricsLoadedRef[0],
                    thresholdsLoadedRef[0],
                    metadata
                );
                
                if (analyzeResp == null || analyzeResp.ctx_id == null || analyzeResp.ctx_id.isBlank()) {
                    throw new IOException("Invalid response from /analyze route.");
                }
                
                // 4) Get smell_id from response (assuming it's returned)
                String smellId = analyzeResp.smell_id != null ? analyzeResp.smell_id : null;
                if (smellId == null || smellId.isBlank()) {
                    throw new IOException("No smell_id in response");
                }
                smellIdRef[0] = smellId;
                
                // 5) Poll for smell by ID (3 second intervals)
                smellResultRef[0] = pollSmellById(smellId, 5, 3000); // 30 attempts, 3 seconds each
                
            } catch (Exception e) {
                exceptionRef[0] = e;
            }
            
            // 6) Update UI (ALWAYS in UI thread)
            Display.getDefault().asyncExec(() -> {
                try {
                    if (exceptionRef[0] != null) {
                        // Handle error case
                        ExecutionLogEntry errorEntry = new ExecutionLogEntry(
                            timestamp, 
                            scriptName, 
                            "N/A", 
                            "Failed: " + exceptionRef[0].getMessage()
                        );
                        addLogToHistory(errorEntry);
                        
                        MessageDialog.openError(
                            getSite().getShell(), 
                            "Error", 
                            "Process failed: " + exceptionRef[0].getMessage()
                        );
                        return;
                    }
                    
                    // Success case - update UI with results
                    if (metricsLoadedRef[0] != null) {
                        MyView.this.metricValues = metricsLoadedRef[0];
                    }
                    if (thresholdsLoadedRef[0] != null) {
                        MyView.this.metricThresholds = thresholdsLoadedRef[0];
                    }
                    
                    // Clear previous content
                    clearMetricsAndChart();
                    
                    // Process smell result
                    SmellResult result = smellResultRef[0];
                    if (result != null) {
                        // Convert SmellResult to DetectionResult for existing methods
                        DetectionResult det = convertToDetectionResult(result);
                        processMetrics(det);
                        
                        // Create log entry
                        String badSmellResult = result.is_smell ? 
                            "Detected: " + result.type : 
                            "No bad smells detected";
                        
                        ExecutionLogEntry logEntry = new ExecutionLogEntry(
                            timestamp, 
                            scriptName, 
                            badSmellResult, 
                            "Success"
                        );
                        addLogToHistory(logEntry);
                        
                        // Show success message
                        MessageDialog.openInformation(
                            getSite().getShell(),
                            "Process Completed",
                            badSmellResult
                        );
                    } else {
                        MessageDialog.openWarning(
                            getSite().getShell(),
                            "Process Completed",
                            "No result available for smell ID: " + smellIdRef[0]
                        );
                    }
                    
                } catch (Exception e) {
                    MessageDialog.openError(
                        getSite().getShell(), 
                        "UI Update Error", 
                        "Error updating interface: " + e.getMessage()
                    );
                } finally {
                    // Always re-enable the button
                    executeButton.setText("Detect Bad Smells");
                    executeButton.setEnabled(true);
                }
            });
            
        }, "SmellDSL-Detection-Thread").start();
    }

    /**
     * Poll for smell by ID with specified intervals
     * @param smellId The smell ID to query
     * @param maxAttempts Maximum number of attempts
     * @param sleepMs Milliseconds between attempts (3000 = 3 seconds)
     * @return The smell result or null if not found
     */
    private SmellResult pollSmellById(String smellId, int maxAttempts, long sleepMs) throws InterruptedException {
        System.out.println("Polling for smell ID: " + smellId + " (" + maxAttempts + " attempts, " + sleepMs/1000 + "s interval)");
        
        for (int i = 0; i < maxAttempts; i++) {
            try {
                SmellResult result = getSmellById(smellId);
                if (result != null) {
                    System.out.println("Got smell result on attempt " + (i+1));
                    return result;
                }
            } catch (IOException e) {
                // Smell not ready yet, continue polling
                System.out.println("Attempt " + (i+1) + "/" + maxAttempts + " - smell not ready yet");
            }
            
            Thread.sleep(sleepMs);
        }
        
        System.out.println("Timeout after " + maxAttempts + " attempts polling for smell: " + smellId);
        return null;
    }

    /**
     * Get smell by ID from the API
     */
    private SmellResult getSmellById(String smellId) throws IOException {
        String url = "http://localhost:5000/smells/" + smellId;
        
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        
        int status = conn.getResponseCode();
        
        if (status == 404) {
            // Smell not found yet - still processing
            return null;
        }
        
        if (status != 200) {
            throw new IOException("Failed to get smell: HTTP " + status);
        }
        
        String responseJson = new BufferedReader(
            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))
            .lines()
            .collect(Collectors.joining("\n"));
        
        Gson gson = new Gson();
        return gson.fromJson(responseJson, SmellResult.class);
    }

    /**
     * Convert SmellResult to DetectionResult for compatibility with existing code
     */
    private DetectionResult convertToDetectionResult(SmellResult result) {
        DetectionResult det = new DetectionResult();
        
        if (result != null) {
            det.isSmell = result.is_smell;
            det.smells = new ArrayList<>();
            if (result.type != null && !result.type.isEmpty()) {
                det.smells.add(result.type);
            }
            
            // Add rule if available
            det.rules = new HashMap<>();
            if (result.rule != null) {
                String ruleStr = result.rule.toString();
                det.rules.put(result.type + "Rule", ruleStr);
            }
            
            // Add treatment
            det.treatments = new HashMap<>();
            if (result.treatment != null) {
                det.treatments.put(result.type, result.treatment);
            }
            
            // Add metrics
            det.metricValues = result.metrics != null ? result.metrics : new HashMap<>();
        }
        
        return det;
    }

    // Add this class for mapping the API response
    private static class SmellResult {
        String id;
        String ctx_id;
        String timestamp_utc;
        String user_id;
        String org_id;
        String loc_id;
        String project_id;
        String type;              // This is the smell name (GodClass)
        String smell_type;         // DesignSmell, ImplementationSmell, etc
        boolean is_smell;
        Object rule;               // Can be String or Map
        String file_path;
        String language;
        String branch;
        String commit_sha;
        String treatment;
        Map<String, Double> metrics;
    }


    private Map<String, double[]> readMetricThresholds(String filePath) throws IOException {
        Map<String, double[]> thresholds = new HashMap<>();
        System.out.println("[DEBUG] Reading thresholds from: " + filePath);
        
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            String header = br.readLine(); // pula o cabeçalho "Metrica,Valor"
            System.out.println("[DEBUG] CSV Header: " + header);
            
            int lineCount = 0;
            while ((line = br.readLine()) != null) {
                lineCount++;
                System.out.println("[DEBUG] Line " + lineCount + ": " + line);
                
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    String metricName = parts[0].trim();
                    double thresholdValue = Double.parseDouble(parts[1].trim());
                    
                    // Armazena o mesmo valor para LOW e MEDIUM (já que só temos um valor)
                    thresholds.put(metricName, new double[]{thresholdValue, thresholdValue});
                    
                    System.out.println("[DEBUG] Added threshold: " + metricName + " = " + thresholdValue);
                } else {
                    System.out.println("[DEBUG] Skipping line - invalid format: " + line);
                }
            }
            System.out.println("[DEBUG] Total thresholds loaded: " + thresholds.size());
        } catch (Exception e) {
            System.out.println("[DEBUG] Error reading thresholds: " + e.getMessage());
            e.printStackTrace();
        }
        
        return thresholds;
    }

    private Map<String, Double> readMetricValues(String filePath) throws IOException {
        Map<String, Double> values = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            br.readLine(); // header
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    String metricName = parts[0].trim();
                    double value = Double.parseDouble(parts[1].trim());
                    values.put(metricName, value);
                }
            }
        }
        return values;
    }

    private String pollStatusJson(String statusBaseUrl, String codCtx) throws IOException, InterruptedException {
        int attempts = 20; // Aumenta para 20 tentativas
        long sleepMs = 1000; // 1 segundo entre tentativas
        
        IOException lastEx = null;
        
        for (int i = 0; i < attempts; i++) {
            try {
                String json = getExecutionStatus(statusBaseUrl, codCtx);
                
                // Se ainda está processing, continua tentando
                if (json != null && !json.isBlank()) {
                    // Verifica se o status ainda é "processing"
                    Gson gson = new Gson();
                    Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
                    Map<String, Object> root = gson.fromJson(json, mapType);
                    
                    if (root.containsKey("status") && "processing".equals(root.get("status"))) {
                        System.out.println("Still processing... attempt " + (i+1) + "/" + attempts);
                        Thread.sleep(sleepMs);
                        continue;
                    }
                    
                    // Tem histórico? Se sim, já pode retornar
                    if (root.containsKey("history") && ((List)root.get("history")).size() > 0) {
                        return json;
                    }
                }
                
                Thread.sleep(sleepMs);
                
            } catch (IOException ex) {
                lastEx = ex;
                Thread.sleep(sleepMs);
            }
        }
        
        if (lastEx != null) throw lastEx;
        throw new IOException("Could not get execution status after " + attempts + " attempts: " + codCtx);
    }

    private static class DetectionResult {
        Boolean isSmell;
        List<String> smells;
        String smellType;                    // Tipo do smell (DesignSmell, etc)
        Map<String, String> rules;            // Mapa de regras por smell
        Map<String, String> treatments;       // Mapa de tratamentos por smell
        Map<String, Double> metricValues;     // Valores das métricas se disponíveis
        
        DetectionResult() {
            this.isSmell = false;
            this.smells = new ArrayList<>();
            this.rules = new HashMap<>();
            this.treatments = new HashMap<>();
            this.metricValues = new HashMap<>();
        }
    }

    private DetectionResult parseDetectionResultFromStatus(String statusJson) {
        DetectionResult det = new DetectionResult();
        det.isSmell = false;
        det.smells = new ArrayList<>();
        
        try {
            Gson gson = new Gson();
            Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> root = gson.fromJson(statusJson, mapType);
            
            // Verifica se tem history
            if (root.containsKey("history") && root.get("history") instanceof List) {
                List<Map<String, Object>> history = (List<Map<String, Object>>) root.get("history");
                
                // Pega o último item (mais recente)
                for (int i = history.size() - 1; i >= 0; i--) {
                    Map<String, Object> item = history.get(i);
                    
                    String status = (String) item.get("status");
                    if (!"INTERPRETED".equalsIgnoreCase(status)) continue;
                    
                    if (item.containsKey("details") && item.get("details") instanceof String) {
                        String detailsStr = (String) item.get("details");
                        
                        // Parse da string details
                        Map<String, Object> details = gson.fromJson(detailsStr, mapType);
                        
                        if (details.containsKey("result") && details.get("result") instanceof Map) {
                            Map<String, Object> result = (Map<String, Object>) details.get("result");
                            
                            // Pega is_smell
                            if (result.containsKey("is_smell")) {
                                det.isSmell = Boolean.TRUE.equals(result.get("is_smell"));
                            }
                            
                            // Pega smells_detected
                            if (result.containsKey("smells_detected") && result.get("smells_detected") instanceof List) {
                                det.smells = (List<String>) result.get("smells_detected");
                            }
                            
                            // Log para debug
                            System.out.println("Parsed result - is_smell: " + det.isSmell);
                            System.out.println("Parsed result - smells: " + det.smells);
                            
                            break; // Encontrou o resultado interpretado
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing status JSON: " + e.getMessage());
            e.printStackTrace();
        }
        
        return det;
    }
    private void extractFromResultObject(com.google.gson.JsonObject resultObj, DetectionResult r) {
        // smells
        if (resultObj.has("smells") && resultObj.get("smells").isJsonArray()) {
            for (var el : resultObj.getAsJsonArray("smells")) {
                if (!el.isJsonNull()) r.smells.add(el.getAsString());
            }
        }

        // opcional: se você quiser usar rules pra construir mensagem
        // (não precisa para detectar, mas ajuda a explicar)
        if (resultObj.has("rules") && resultObj.get("rules").isJsonObject()) {
            // você pode ler depois se quiser
        }

        // opcional: treatments também
        if (resultObj.has("treatments") && resultObj.get("treatments").isJsonObject()) {
            // você pode ler depois se quiser
        }

        // isSmell não vem explícito, então inferimos:
        r.isSmell = !r.smells.isEmpty();
    }


    
    /**
>>>>>>> 23283ea (feat: Adiciona histórico de execuções e refatora a interface da view)
     * Cria a tabela para exibir as métricas e suas classificações.
     * @param parent O composite pai.
     */
    /*private void createMetricsTable(Composite parent) {
        metricsTable = new Table(parent, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        metricsTable.setHeaderVisible(true);
        metricsTable.setLinesVisible(true);
        metricsTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        metricsTable.setFont(new Font(parent.getDisplay(), "Segoe UI", 9, SWT.NORMAL));
        metricsTable.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));

        String[] colunas = {"Métrica", "Valor", "Classificação"};
        for (String col : colunas) {
            TableColumn column = new TableColumn(metricsTable, SWT.LEFT);
            column.setText(col);
        }
    }
*/
    private void createMetricsTable(Composite parent) {
        metricsTable = new Table(parent, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        metricsTable.setHeaderVisible(true);
        metricsTable.setLinesVisible(true);
        metricsTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        metricsTable.setFont(new Font(parent.getDisplay(), "Segoe UI", 9, SWT.NORMAL));

        String[] columns = {
            "Smell",
            "Type",
            "Detected",
            "Rule",
            "Treatment"
        };

        for (String col : columns) {
            TableColumn column = new TableColumn(metricsTable, SWT.LEFT);
            column.setText(col);
            column.setWidth(120);
        }
    }
    /**
     * Cria o composite que servirá como container para o gráfico.
     * @param parent O composite pai.
     */
    private void createChartArea(Composite parent) {
        chartComposite = new Composite(parent, SWT.NONE);
        chartComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        chartComposite.setLayout(new FillLayout());
    }
    
    /**
     * Cria a seção de histórico de execuções com uma tabela e um botão para limpar.
     * @param parent O composite pai.
     */
    private void createHistoryArea(Composite parent) {
        Group historyGroup = new Group(parent, SWT.NONE);
        historyGroup.setText("Execution History");
        historyGroup.setLayout(new GridLayout(1, false));
        historyGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        historyGroup.setFont(new Font(parent.getDisplay(), "Segoe UI", 9, SWT.BOLD));

        historyTable = new Table(historyGroup, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
        historyTable.setHeaderVisible(true);
        historyTable.setLinesVisible(true);
        historyTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        String[] headers = {"Date - Time", "Script", "Bad Smells", "Status"};
        for (String header : headers) {
            TableColumn column = new TableColumn(historyTable, SWT.NONE);
            column.setText(header);
            column.setWidth(150);
        }

        Button clearButton = new Button(historyGroup, SWT.PUSH);
        clearButton.setText("Clear History");
        clearButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        clearButton.addListener(SWT.Selection, e -> clearHistory());
    }
    /**
     * Carrega os limites (thresholds) de cada métrica a partir de um arquivo CSV.
     * O formato esperado é: Metrica,LimiteLow,LimiteMedium
     * @param filePath O caminho para o arquivo CSV de limites.
     */
    private void loadMetricThresholds(String filePath) throws IOException, NumberFormatException {
        metricThresholds = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;

            br.readLine(); // pula o cabeçalho

            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");

                if (parts.length >= 3) {
                    String metricName = parts[0].trim();
                    double lowThreshold = Double.parseDouble(parts[1].trim());
                    double mediumThreshold = Double.parseDouble(parts[2].trim());

                    metricThresholds.put(metricName, new double[]{lowThreshold, mediumThreshold});
                }
            }
        }
    }
    /**
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 51a1313 (inicial commit)
     * Lê os dados de métricas de um arquivo CSV, processa e então gera a tabela e o gráfico.
     * O formato esperado é: Metrica,Valor
     * @param filePath O caminho para o arquivo CSV de valores.
     */
    private void loadMetricValuesAndProcess(String filePath) throws IOException, NumberFormatException {

        metricValues = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

            String line;
            br.readLine(); // skip header

            while ((line = br.readLine()) != null) {

                String[] parts = line.split(",");

                if (parts.length >= 2) {

                    String metricName = parts[0].trim();
                    double value = Double.parseDouble(parts[1].trim());

                    metricValues.put(metricName, value);
                }
            }
        }

        // limpa UI
        clearMetricsAndChart();

  
    }

    /**
<<<<<<< HEAD
=======
>>>>>>> 23283ea (feat: Adiciona histórico de execuções e refatora a interface da view)
=======
>>>>>>> 51a1313 (inicial commit)
     * Limpa o conteúdo da tabela de métricas e remove o gráfico anterior.
     */
    private void clearMetricsAndChart() {
        metricsTable.removeAll();
        for (Control child : chartComposite.getChildren()) child.dispose();
        if(chartComposite != null && !chartComposite.isDisposed()){
            chartComposite.layout();
        }
    }

    /**
     * Processa os valores das métricas, preenche a tabela e prepara os dados para o gráfico.
     */
    /*private void processMetrics() {
        List<String> sortedMetrics = metricValues.keySet().stream().sorted().collect(Collectors.toList());
        double[] numericClassifications = new double[sortedMetrics.size()];

        int i = 0;
        for (String metric : sortedMetrics) {
            double value = metricValues.get(metric);
            String classification = classify(metric, value);
            addMetricTableRow(metric, String.valueOf(value), classification);

            switch (classification) {
                case "LOW":
                    numericClassifications[i] = 1.0;
                    break;
                case "MEDIUM":
                    numericClassifications[i] = 2.0;
                    break;
                case "HIGH":
                    numericClassifications[i] = 3.0;
                    break;
                default: // UNKNOWN
                    numericClassifications[i] = 0.0;
                    break;
            }
            i++;
        }

        for (TableColumn col : metricsTable.getColumns()) {
            col.pack();
        }

        generateChart(sortedMetrics.toArray(new String[0]), numericClassifications);
    }*/
    
    private void processMetrics(DetectionResult det) {
        System.out.println("=== START processMetrics ===");
        
        if (metricsTable == null || metricsTable.isDisposed()) {
            System.out.println("ERROR: metricsTable is disposed");
            return;
        }
        
        metricsTable.removeAll();
        
        if (det != null && det.smells != null && !det.smells.isEmpty()) {
            // Mostra os smells detectados com dados REAIS
            for (String smell : det.smells) {
                TableItem item = new TableItem(metricsTable, SWT.NONE);
                
                // Aqui você pode buscar regras e tratamentos específicos para cada smell
                String rule = getRuleForSmell(smell, det);
                String treatment = getTreatmentForSmell(smell, det);
                
                item.setText(new String[]{
                    smell,                                   // Nome do smell
                    det.smellType != null ? det.smellType : "DesignSmell",  // Tipo (se disponível)
                    det.isSmell != null && det.isSmell ? "YES" : "NO",      // Detectado
                    rule,                                    // Regra aplicada
                    treatment                                 // Tratamento
                });
                
                // Color coding
                if (det.isSmell != null && det.isSmell) {
                    item.setForeground(redColor);
                }
            }
        } else if (metricValues != null && !metricValues.isEmpty()) {
            // Fallback: mostra métricas
            List<String> sortedMetrics = new ArrayList<>(metricValues.keySet());
            Collections.sort(sortedMetrics);
            
            for (String metric : sortedMetrics) {
                Double value = metricValues.get(metric);
                TableItem item = new TableItem(metricsTable, SWT.NONE);
                item.setText(new String[]{
                    metric,
                    "Metric",
                    "NO",
                    String.valueOf(value),
                    "-"
                });
            }
        } else {
            TableItem item = new TableItem(metricsTable, SWT.NONE);
            item.setText(new String[]{
                "No data available",
                "-",
                "-",
                "-",
                "-"
            });
        }
        
        for (TableColumn col : metricsTable.getColumns()) {
            col.pack();
        }
        
        // Generate chart based on detection - chamar apenas UM método
        if (det != null && det.smells != null && !det.smells.isEmpty()) {
            generateChartFromResults(det);  // ← apenas este, o outro é redundante
        } else {
            generateRiskChart(false);
        }
        
        System.out.println("=== END processMetrics ===");
    }

    /**
     * Busca a regra específica para um smell
     */
    private String getRuleForSmell(String smellName, DetectionResult det) {
        // Se você tiver um mapa de regras no DetectionResult, use-o
        if (det != null && det.rules != null && det.rules.containsKey(smellName)) {
            return det.rules.get(smellName);
        }
        
        // Fallback: constrói nome da regra
        return "{\"" + smellName + "Rule\": " + (det.isSmell ? "true" : "false") + "}";
    }

    /**
     * Busca o tratamento específico para um smell
     */
    private String getTreatmentForSmell(String smellName, DetectionResult det) {
        // Se você tiver um mapa de tratamentos no DetectionResult, use-o
        if (det != null && det.treatments != null && det.treatments.containsKey(smellName)) {
            return det.treatments.get(smellName);
        }
        
        // Fallback: tratamento genérico
        return "Refactor into smaller classes";
    }

    private void generateChartFromResults(DetectionResult det) {
        // Limpa gráfico anterior
        for (Control child : chartComposite.getChildren()) {
            child.dispose();
        }
        
        if (det == null || det.smells == null || det.smells.isEmpty()) {
            return;
        }
        
        Display display = chartComposite.getDisplay();
        Chart chart = new Chart(chartComposite, SWT.NONE);
        
        chart.setBackground(display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        chart.getTitle().setText("Smell Detection Results");
        chart.getTitle().setFont(new Font(display, "Segoe UI", 11, SWT.BOLD));
        
        // Prepara dados - usa valores REAIS se disponíveis
        String[] categories = det.smells.toArray(new String[0]);
        double[] values = new double[det.smells.size()];
        
        for (int i = 0; i < det.smells.size(); i++) {
            String smell = det.smells.get(i);
            
            // Se você tiver valores numéricos para cada smell, use-os
            if (det.metricValues != null && det.metricValues.containsKey(smell)) {
                values[i] = det.metricValues.get(smell);
            } else {
                // Fallback: 3.0 para detectado, 1.0 para não detectado
                values[i] = (det.isSmell != null && det.isSmell) ? 3.0 : 1.0;
            }
        }
        
        // Configura eixos
        IAxisSet axisSet = chart.getAxisSet();
        
        IAxis xAxis = axisSet.getXAxis(0);
        xAxis.setCategorySeries(categories);
        xAxis.enableCategory(true);
        xAxis.getTitle().setText("Smells");
        
        IAxis yAxis = axisSet.getYAxis(0);
        yAxis.getTitle().setText("Detection Status / Value");
        yAxis.setRange(new Range(0, 4));
        
        // Cria série de barras
        IBarSeries series = (IBarSeries) chart.getSeriesSet()
            .createSeries(SeriesType.BAR, "Smells");
        series.setYSeries(values);
        
        // Cor baseada na detecção
        Color barColor = (det.isSmell != null && det.isSmell) ? 
            new Color(display, 244, 67, 54) :  // Vermelho
            new Color(display, 76, 175, 80);    // Verde
        series.setBarColor(barColor);
        
        axisSet.adjustRange();
        chartComposite.layout();
    }
    
    private void generateChartFromSmells(List<String> smells, Boolean isSmell) {
        // Clear previous chart
        for (Control child : chartComposite.getChildren()) {
            child.dispose();
        }
        
        if (smells == null || smells.isEmpty()) {
            return;
        }
        
        Display display = chartComposite.getDisplay();
        Chart chart = new Chart(chartComposite, SWT.NONE);
        
        // Style chart
        chart.setBackground(display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        chart.getTitle().setText("Smell Detection Results");
        chart.getTitle().setFont(new Font(display, "Segoe UI", 11, SWT.BOLD));
        
        // Prepare data
        String[] categories = smells.toArray(new String[0]);
        double[] values = new double[smells.size()];
        Arrays.fill(values, isSmell != null && isSmell ? 3.0 : 1.0);
        
        // Configure axes
        IAxisSet axisSet = chart.getAxisSet();
        
        IAxis xAxis = axisSet.getXAxis(0);
        xAxis.setCategorySeries(categories);
        xAxis.enableCategory(true);
        
        IAxis yAxis = axisSet.getYAxis(0);
        yAxis.getTitle().setText("Detection Status");
        yAxis.setRange(new Range(0, 4));
        
        // Create bar series
        IBarSeries series = (IBarSeries) chart.getSeriesSet()
            .createSeries(SeriesType.BAR, "Smells");
        series.setYSeries(values);
        
        // Color based on detection
        Color barColor = (isSmell != null && isSmell) ? 
            new Color(display, 244, 67, 54) :  // Red
            new Color(display, 76, 175, 80);    // Green
        series.setBarColor(barColor);
        
        axisSet.adjustRange();
        chartComposite.layout();
    }

    /**
     * Display metrics with risk levels (fallback)
     */
    private void displayMetricsWithRiskLevels(boolean isSmell) {
        List<String> sortedMetrics = new ArrayList<>(metricValues.keySet());
        Collections.sort(sortedMetrics);
        
        for (String metric : sortedMetrics) {
            Double valueObj = metricValues.get(metric);
            double value = valueObj != null ? valueObj : 0.0;
            
            String classification = classify(metric, value);
            
            TableItem item = new TableItem(metricsTable, SWT.NONE);
            item.setText(new String[]{
                metric,
                String.format("%.2f", value),
                isSmell ? "YES" : "NO",
                classification,
                "-"
            });
            
            // Color coding based on risk
            if ("HIGH".equals(classification)) {
                item.setForeground(redColor);
            } else if ("MEDIUM".equals(classification)) {
                item.setForeground(yellowColor);
            } else if ("LOW".equals(classification)) {
                item.setForeground(greenColor);
            }
        }
    }

    /**
     * Generate risk level chart
     */
    private void generateRiskChart(boolean isSmell) {
            // Clear previous chart
            for (Control child : chartComposite.getChildren()) {
                child.dispose();
            }
            
            if (metricValues == null || metricValues.isEmpty()) {
                return;
            }
            
            Display display = chartComposite.getDisplay();
            Chart chart = new Chart(chartComposite, SWT.NONE);
            
            // Style chart
            chart.setBackground(display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
            chart.getTitle().setText("Metric Risk Analysis");
            chart.getTitle().setFont(new Font(display, "Segoe UI", 11, SWT.BOLD));
            
            // Prepare data
            List<String> sortedMetrics = new ArrayList<>(metricValues.keySet());
            Collections.sort(sortedMetrics);
            
            String[] categories = sortedMetrics.toArray(new String[0]);
            double[] riskLevels = new double[categories.length];
            
            for (int i = 0; i < categories.length; i++) {
                String metric = categories[i];
                Double value = metricValues.get(metric);
                String classification = classify(metric, value != null ? value : 0.0);
                
                switch (classification) {
                    case "LOW":
                        riskLevels[i] = 1.0;
                        break;
                    case "MEDIUM":
                        riskLevels[i] = 2.0;
                        break;
                    case "HIGH":
                        riskLevels[i] = 3.0;
                        break;
                    default:
                        riskLevels[i] = 0.0;
                }
            }
            
            // Configure axes
            IAxisSet axisSet = chart.getAxisSet();
            
            IAxis xAxis = axisSet.getXAxis(0);
            xAxis.setCategorySeries(categories);
            xAxis.enableCategory(true);
            
            IAxis yAxis = axisSet.getYAxis(0);
            yAxis.getTitle().setText("Risk Level");
            yAxis.setRange(new Range(0, 4));
            
            // Create bar series
            IBarSeries series = (IBarSeries) chart.getSeriesSet()
                .createSeries(SeriesType.BAR, "Metrics");
            series.setYSeries(riskLevels);
            
            // Set color based on smell detection
            Color barColor = isSmell ? 
                new Color(display, 244, 67, 54) : // Red for smells
                new Color(display, 76, 175, 80);   // Green for no smells
            series.setBarColor(barColor);
            
            axisSet.adjustRange();
            chartComposite.layout();
        }

    /**
     * Show message when no metrics are available
     */
    private void showNoMetricsMessage() {
        if (metricsTable == null || metricsTable.isDisposed()) return;
        
        metricsTable.removeAll();
        TableItem item = new TableItem(metricsTable, SWT.NONE);
        item.setText(new String[]{
            "No metrics loaded",
            "-",
            "-",
            "-",
            "-"
        });
        item.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
        
        for (TableColumn col : metricsTable.getColumns()) {
            col.pack();
        }
    }

    /**
     * Show error message in table
     */
    private void showErrorMessageInTable(String message) {
        if (metricsTable == null || metricsTable.isDisposed()) return;
        
        metricsTable.removeAll();
        TableItem item = new TableItem(metricsTable, SWT.NONE);
        item.setText(new String[]{
            message,
            "-",
            "-",
            "-",
            "-"
        });
        item.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
        
        for (TableColumn col : metricsTable.getColumns()) {
            col.pack();
        }
    }

    /**
     * Helper method to show error in table
     */
    private void showErrorInTable(String message, boolean isSmell) {
        try {
            if (metricsTable != null && !metricsTable.isDisposed()) {
                metricsTable.removeAll();
                TableItem item = new TableItem(metricsTable, SWT.NONE);
                item.setText(new String[]{
                    "ERROR: " + message,
                    "-",
                    isSmell ? "YES" : "NO",
                    "-",
                    "-"
                });
                
                // Change color to red
                item.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
                
                for (TableColumn col : metricsTable.getColumns()) {
                    col.pack();
                }
            }
        } catch (Exception e) {
            System.out.println("ERROR showing message in table: " + e.getMessage());
        }
    }

    /**
     * Adiciona uma nova linha à tabela de métricas.
     * @param metrica O nome da métrica.
     * @param valor O valor da métrica.
     * @param classificacao A classificação (LOW, MEDIUM, HIGH).
     */
    private void addMetricTableRow(String metric, String value, String is_smell) {
        TableItem item = new TableItem(metricsTable, SWT.NONE);
        item.setText(new String[]{metric, value, is_smell});
        item.setForeground(fontColor);
    }

    /**
     * Gera e estiliza o gráfico de barras com base nos valores das métricas.
<<<<<<< HEAD
<<<<<<< HEAD
     * @param metricNames Os nomes das métricas para o eixo X.
     * @param valoresNumericos Array de dados para a série.
     */
    private void generateChart(String[] metricNames, double[] valoresNumericos, boolean isSmell) {

        Display display = chartComposite.getDisplay();

        Chart chart = new Chart(chartComposite, SWT.NONE);

        chart.setBackground(lightGrayBackground);

        chart.getTitle().setText("Metric Risk Classification");
        chart.getTitle().setFont(new Font(display, "Segoe UI", 11, SWT.BOLD));
        chart.getTitle().setForeground(fontColor);

        IAxisSet axisSet = chart.getAxisSet();

        IAxis yAxis = axisSet.getYAxis(0);

        yAxis.getTitle().setText("Risk Level");
        yAxis.setRange(new Range(0, 4));

        IAxis xAxis = axisSet.getXAxis(0);

        xAxis.setCategorySeries(metricNames);
        xAxis.enableCategory(true);

        IBarSeries series = (IBarSeries) chart
                .getSeriesSet()
                .createSeries(SeriesType.BAR, "Metrics");

        series.setYSeries(valoresNumericos);

        // cor dinâmica
        Color barColor;

        if (isSmell) {
            barColor = new Color(display, 200, 0, 0); // vermelho
        } else {
            barColor = new Color(display, 0, 150, 0); // verde
        }

        series.setBarColor(barColor);

        axisSet.adjustRange();

        chartComposite.layout();
    }

    /**
<<<<<<< HEAD
<<<<<<< HEAD
     * Classifica um valor de métrica como LOW, MEDIUM ou HIGH com base nos limiares carregados.
=======
     * Classifica um valor de métrica como LOW, MEDIUM ou HIGH com base em limiares pré-definidos.
>>>>>>> 23283ea (feat: Adiciona histórico de execuções e refatora a interface da view)
=======
     * Classifica um valor de métrica como LOW, MEDIUM ou HIGH com base nos limiares carregados.
>>>>>>> 51a1313 (inicial commit)
     * @param metrica O nome da métrica.
     * @param valor O valor a ser classificado.
     * @return A string de classificação.
     */
private String classify(String metrica, double valor) {

    if (metricThresholds == null || !metricThresholds.containsKey(metrica)) {
        return "UNKNOWN";
    }

    double[] thresholds = metricThresholds.get(metrica);

    if (valor <= thresholds[0]) {
        return "LOW";
    } else if (valor <= thresholds[1]) {
        return "MEDIUM";
    } else {
        return "HIGH";
    }
}

    /**
     * Obtém o caminho completo para o arquivo JSON de histórico.
     * @return O caminho do arquivo.
     */
    private String getHistoryFilePath() {
        Bundle bundle = Platform.getBundle("Smelldsl"); 
        return Platform.getStateLocation(bundle).append(HISTORY_FILE_NAME).toOSString();
    }

    /**
     * Carrega as entradas de histórico do arquivo JSON para a tabela da UI.
     */
    private void loadHistory() {
        String filePath = getHistoryFilePath();
        if (!new File(filePath).exists()) return;

        try {
            String json = new String(Files.readAllBytes(Paths.get(filePath)));
            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<ExecutionLogEntry>>(){}.getType();
            List<ExecutionLogEntry> history = gson.fromJson(json, listType);

            if (history != null) {
                for (ExecutionLogEntry entry : history) {
                    addHistoryTableRow(entry);
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading records: " + e.getMessage());
        }
    }
    
    /**
     * Adiciona uma nova entrada de log à UI e ao arquivo JSON de histórico.
     * @param logEntry O objeto de log a ser adicionado.
     */
    private void addLogToHistory(ExecutionLogEntry logEntry) {
        addHistoryTableRow(logEntry); // Adiciona na UI
        
        // Adiciona no arquivo JSON
        String filePath = getHistoryFilePath();
        Gson gson = new Gson();
        List<ExecutionLogEntry> history = new ArrayList<>();
        
        try {
            if (new File(filePath).exists()) {
                String json = new String(Files.readAllBytes(Paths.get(filePath)));
                Type listType = new TypeToken<ArrayList<ExecutionLogEntry>>(){}.getType();
                history = gson.fromJson(json, listType);
                if (history == null) history = new ArrayList<>();
            }
            history.add(logEntry);
            try (FileWriter writer = new FileWriter(filePath)) {
                gson.toJson(history, writer);
            }
        } catch (IOException e) {
             System.err.println("Error saving history: " + e.getMessage());
        }
    }
    
    /**
     * Adiciona uma nova linha à tabela de histórico na UI.
     * @param entry A entrada de log a ser exibida.
     */
    private void addHistoryTableRow(ExecutionLogEntry entry) {
        TableItem item = new TableItem(historyTable, SWT.NONE);
        item.setText(new String[]{entry.timestamp, entry.script, entry.badSmells, entry.status});
        for (TableColumn col : historyTable.getColumns()) {
            col.pack();
        }
    }
    
    /**
     * Limpa a tabela de histórico na UI e deleta o arquivo JSON correspondente.
     */
    private void clearHistory() {
        boolean confirmed = MessageDialog.openConfirm(
            getSite().getShell(),
            "Confirm Clearing",
            "Are you sure you want to clear the entire execution history? This action cannot be undone."
        );

        if (confirmed) {
            historyTable.removeAll();
            try {
                Files.deleteIfExists(Paths.get(getHistoryFilePath()));
            } catch (IOException e) {
                MessageDialog.openError(getSite().getShell(), "Error", "The history file could not be deleted.");
            }
        }
    }
    
    @Override
    public void setFocus() {
        if(metricsTable != null && !metricsTable.isDisposed()) {
            metricsTable.setFocus();
        }
    }

    @Override
    public void dispose() {
        if (greenColor != null) greenColor.dispose();
        if (yellowColor != null) yellowColor.dispose();
        if (redColor != null) redColor.dispose();
        if (primaryBlue != null) primaryBlue.dispose();
        if (lightGrayBackground != null) lightGrayBackground.dispose();
        super.dispose();
    }

    private AnalyzeResponse callAnalyzeApi(
            String apiUrl,
            String dslCode,
            Map<String, Double> metricsLoaded,
            Map<String, double[]> thresholdsLoaded,
            Map<String, Object> metadata
    ) throws IOException {

        Gson gson = new Gson();

        Map<String, Object> payload = new HashMap<>();
        
        Map<String, Double> thresholdsForApi = new HashMap<>();
        for (Map.Entry<String, double[]> entry : thresholdsLoaded.entrySet()) {
            String metricName = entry.getKey();
            double[] values = entry.getValue();
            thresholdsForApi.put(metricName, values[1]); 
        }
        payload.put("user_id", metadata.get("user_id"));
        payload.put("smell_dsl", dslCode);
        payload.put("metrics", metricsLoaded);
        payload.put("thresholds", thresholdsForApi);
        
        // METADADOS agrupados
        payload.put("request_data", metadata);

        String jsonBody = gson.toJson(payload);
        
        System.out.println("[PLUGIN] Payload enviado:");
        System.out.println(jsonBody);

        HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        InputStream is = status < 300 ? conn.getInputStream() : conn.getErrorStream();

        String responseJson = new BufferedReader(new InputStreamReader(is))
                .lines()
                .collect(Collectors.joining("\n"));

        if (status >= 300) {
            throw new IOException("HTTP Error " + status + ": " + responseJson);
        }

        // APENAS retorna a resposta do /analyze
        return gson.fromJson(responseJson, AnalyzeResponse.class);
    }
    
    private String extractPrimarySmellName(String dslCode) {
        if (dslCode == null) return null;

        // pega o primeiro "smell Nome"
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?m)^\\s*smell\\s+([A-Za-z_][A-Za-z0-9_]*)\\b")
                .matcher(dslCode);

        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    private Map<String, Object> buildEnvFromMetricsAndThresholds(
            Map<String, Double> metrics,
            Map<String, double[]> thresholds,
            String smellName
    ) {
        Map<String, Object> env = new HashMap<>();

        // prefixo do smell (CompositeGodClass.)
        String prefix = (smellName != null && !smellName.isBlank()) ? smellName + "." : "";

        // 1) métricas: CompositeGodClass.LOC, etc.
        if (metrics != null) {
            for (Map.Entry<String, Double> e : metrics.entrySet()) {
                env.put(prefix + e.getKey(), e.getValue());
            }
        }

        // 2) thresholds: LOC.LOW, LOC.MEDIUM, LOC.HIGH, etc.
        // Seu CSV tem (low, medium). Para HIGH, usamos medium como fallback
        if (thresholds != null) {
            for (Map.Entry<String, double[]> e : thresholds.entrySet()) {
                String metric = e.getKey();
                double low = e.getValue()[0];
                double medium = e.getValue()[1];
//                double high = e.getValue()[2];
                
                env.put(metric + ".LOW", low);
                env.put(metric + ".MEDIUM", medium);

                // fallback seguro: se não há coluna HIGH no CSV, HIGH = MEDIUM
                env.put(metric + ".HIGH", medium);
            }
        }

        return env;
    }



    private String getExecutionStatus(String statusBaseUrl, String codCtx) throws IOException {
        URL url = new URL(statusBaseUrl + codCtx);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            return br.lines().collect(Collectors.joining("\n"));
        }
    }
    
    

    private static class AnalyzeRequest {
        String id;                       // top-level obrigatório
        Header header;

        // compatibilidade (você já usa)
        String file_content;

        // formato preferido /analyze em várias versões
        String smell_dsl;

        // ✅ NOVO: métricas que o interpretador usa para avaliar as regras
        Map<String, Object> env;
    }


	private static class Header {
	    String codCompany;
	    String codContext;
	    String companyName;
	    String name;
	    String smell_type;
	    boolean is_smell;
	    String timestamp;
	}

	private static class AnalyzeResponse {
	    String ctx_id;
	    String smell_id;
	    String status;

	}

	private static class Result {
	    boolean interpreted;
	    List<String> smells;
	    Object rules;
	    Object treatments;
	}



}
