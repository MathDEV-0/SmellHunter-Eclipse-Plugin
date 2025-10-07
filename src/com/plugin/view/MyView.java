package com.plugin.view;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
<<<<<<< HEAD
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
=======
import java.lang.reflect.Type;
>>>>>>> 23283ea (feat: Adiciona histórico de execuções e refatora a interface da view)
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
<<<<<<< HEAD
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
=======
import java.util.List;
import java.util.stream.IntStream;
>>>>>>> 23283ea (feat: Adiciona histórico de execuções e refatora a interface da view)

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
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

<<<<<<< HEAD
    // Armazena os dados das métricas e seus limites carregados dos arquivos
    private Map<String, Double> metricValues;
    private Map<String, double[]> metricThresholds;
    
    private static final String HISTORY_FILE_NAME = "execution_history.json";

    // Cores personalizadas para a UI
    private Color greenColor, redColor, yellowColor, primaryBlue, lightGrayBackground, fontColor;
=======
    private final String[] metricas = {"LOC", "NOA", "NOM", "WMC", "LCOM", "CBO", "DIT", "NOC"};
    private static final String HISTORY_FILE_NAME = "execution_history.json";

    // Cores personalizadas para a UI
    private Color greenColor, redColor, primaryBlue, lightGrayBackground, fontColor;
>>>>>>> 23283ea (feat: Adiciona histórico de execuções e refatora a interface da view)
    
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
<<<<<<< HEAD
        greenColor = new Color(display, 76, 175, 80);   // Verde para LOW
        yellowColor = new Color(display, 255, 193, 7); // Amarelo para MEDIUM
        redColor = new Color(display, 244, 67, 54);     // Vermelho para HIGH
=======
        greenColor = new Color(display, 76, 175, 80);
        redColor = new Color(display, 244, 67, 54);
>>>>>>> 23283ea (feat: Adiciona histórico de execuções e refatora a interface da view)
        primaryBlue = new Color(display, 33, 150, 243);
        lightGrayBackground = new Color(display, 245, 245, 245);
        fontColor = display.getSystemColor(SWT.COLOR_DARK_GRAY);
    }

    /**
     * Cria a seção de gerenciamento de arquivos com slots para upload e o botão de execução.
     * @param parent O composite pai.
     */
    private void createFileManagementArea(Composite parent) {
    	Group fileGroup = new Group(parent, SWT.NONE);
        fileGroup.setText("Gerenciamento de Arquivos");
        fileGroup.setLayout(new GridLayout(5, false));
        fileGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        fileGroup.setFont(new Font(parent.getDisplay(), "Segoe UI", 9, SWT.BOLD));
        
        GridLayout groupLayout = (GridLayout) fileGroup.getLayout();
        groupLayout.marginWidth = 10;
        groupLayout.marginHeight = 10;
        groupLayout.verticalSpacing = 8;

        createFileSlot(fileGroup, 0, "Especificações (*.smelldsl)", new String[]{"*.smelldsl"});
        createFileSlot(fileGroup, 1, "Valores Métrica (*.csv, *.json)", new String[]{"*.csv", "*.json"});
        createFileSlot(fileGroup, 2, "Limites Métricas (*.csv, *.json)", new String[]{"*.csv", "*.json"});

        executeButton = new Button(fileGroup, SWT.PUSH);
        executeButton.setText("Detectar Badsmells");
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
        attachButton.setText("Anexar...");
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
        deleteButton.setText("Excluir");
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
<<<<<<< HEAD
    }
    
    /**
     * Ponto de entrada para o processo de detecção. É chamado quando o botão "Detectar" é clicado.
     */
    private void executeDetectionProcess() {
        if (!executeButton.isEnabled()) {
            MessageDialog.openError(getSite().getShell(), "Erro", "Todos os arquivos devem ser carregados.");
            return;
        }
        executeButton.setText("Processando...");
        executeButton.setEnabled(false);

        new Thread(() -> {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            String scriptName = new File(filePaths[0]).getName();

            try {
                // 1) I/O e processamento OFF-UI THREAD (seguro)
                Map<String, double[]> thresholdsLoaded = readMetricThresholds(filePaths[2]);
                Map<String, Double> metricsLoaded = readMetricValues(filePaths[1]);

                String dslCode = new String(
                    Files.readAllBytes(Paths.get(filePaths[0])),
                    java.nio.charset.StandardCharsets.UTF_8
                );

                // 2) Chama API (contrato real): /analyze -> execution_id
                String analyzeUrl = "http://localhost:5000/analyze";
                AnalyzeResponse analyzeResp = callAnalyzeApi(analyzeUrl, dslCode, metricsLoaded, thresholdsLoaded);

                if (analyzeResp == null || analyzeResp.execution_id == null || analyzeResp.execution_id.isBlank()) {
                    throw new IOException("Resposta inválida do /analyze: execution_id ausente.");
                }

                // 3) Consulta /status/<cod_ctx> (pode ser assíncrono: faz polling curto)
                String statusJson = pollStatusJson("http://localhost:5000/status/", analyzeResp.execution_id);
                
                // 4) Extrai resultado do JSON de status (robusto: tenta smells_detected e is_smell)
                DetectionResult det = parseDetectionResultFromStatus(statusJson);

                String badSmellResult;
                if (det.smells != null && !det.smells.isEmpty()) {
                    badSmellResult = "Detectados: " + String.join(", ", det.smells);
                } else if (Boolean.TRUE.equals(det.isSmell)) {
                    badSmellResult = "Bad smell detectado";
                } else {
                    badSmellResult = "Nenhum bad smell detectado";
                }

                ExecutionLogEntry logEntry = new ExecutionLogEntry(timestamp, scriptName, badSmellResult, "Sucesso");

                // 5) ATUALIZA UI no UI THREAD (SWT-safe)
                Display.getDefault().asyncExec(() -> {
                    try {
                        // Atualiza estado do objeto (usado por processMetrics/classify)
                        this.metricThresholds = thresholdsLoaded;
                        this.metricValues = metricsLoaded;

                        // Recalcula tabela e gráfico usando seus métodos (UI thread!)
                        clearMetricsAndChart();
                        processMetrics();

                        addLogToHistory(logEntry);

                        MessageDialog.openInformation(
                            getSite().getShell(),
                            "Processo Concluído",
                            badSmellResult
                        );
                    } finally {
                    	executeButton.setText("Detectar Badsmells");
                    	executeButton.setEnabled(true);
                    }
                });

            } catch (Exception e) {
                ExecutionLogEntry logEntry = new ExecutionLogEntry(timestamp, scriptName, "N/A", "Falha");

                Display.getDefault().asyncExec(() -> {
                    addLogToHistory(logEntry);
                    MessageDialog.openError(getSite().getShell(), "Erro", "Falha ao processar: " + e.getMessage());
                    executeButton.setEnabled(true);
                });
            }
        }, "SmellDSL-Detection-Thread").start();
    }

    private Map<String, double[]> readMetricThresholds(String filePath) throws IOException {
        Map<String, double[]> thresholds = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            br.readLine(); // header
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    String metricName = parts[0].trim();
                    double lowThreshold = Double.parseDouble(parts[1].trim());
                    double mediumThreshold = Double.parseDouble(parts[2].trim());
                    thresholds.put(metricName, new double[]{lowThreshold, mediumThreshold});
                }
            }
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
        // Tenta por ~5s (10 tentativas * 500ms). Ajuste se quiser.
        int attempts = 10;
        long sleepMs = 500;

        IOException lastEx = null;

        for (int i = 0; i < attempts; i++) {
            try {
            	String json = getExecutionStatus(statusBaseUrl, codCtx);
                if (json != null && !json.isBlank()) {
                    // Se você tiver um campo "status" tipo DONE/PROCESSING, dá para checar aqui.
                    return json;
                }
            } catch (IOException ex) {
                lastEx = ex;
            }
            Thread.sleep(sleepMs);
        }

        if (lastEx != null) throw lastEx;
        throw new IOException("Não foi possível obter status da execução: " + codCtx);
    }

    private static class DetectionResult {
        Boolean isSmell;
        List<String> smells;
    }

    private DetectionResult parseDetectionResultFromStatus(String statusJson) {
        DetectionResult r = new DetectionResult();
        r.isSmell = false;
        r.smells = new ArrayList<>();

        try {
            com.google.gson.JsonObject root = com.google.gson.JsonParser.parseString(statusJson).getAsJsonObject();

            // 1) Caso ideal: result direto
            if (root.has("result") && root.get("result").isJsonObject()) {
                extractFromResultObject(root.getAsJsonObject("result"), r);
                return r;
            }

            // 2) Caso real do seu /status: history[].details contém uma string JSON com {"result": {...}}
            if (root.has("history") && root.get("history").isJsonArray()) {
                com.google.gson.JsonArray hist = root.getAsJsonArray("history");

                // busca o último item INTERPRETED (melhor que pegar o primeiro)
                for (int i = hist.size() - 1; i >= 0; i--) {
                    if (!hist.get(i).isJsonObject()) continue;
                    com.google.gson.JsonObject item = hist.get(i).getAsJsonObject();

                    String st = item.has("status") && !item.get("status").isJsonNull()
                            ? item.get("status").getAsString()
                            : "";

                    if (!"INTERPRETED".equalsIgnoreCase(st)) continue;

                    if (item.has("details") && !item.get("details").isJsonNull()) {
                        // details pode vir como string JSON
                        String detailsRaw = item.get("details").getAsString();

                        com.google.gson.JsonObject detailsObj =
                                com.google.gson.JsonParser.parseString(detailsRaw).getAsJsonObject();

                        if (detailsObj.has("result") && detailsObj.get("result").isJsonObject()) {
                            extractFromResultObject(detailsObj.getAsJsonObject("result"), r);
                            return r;
                        }
                    }
                }
            }

            // 3) fallback: tenta smells_detected ou is_smell no topo (se existir)
            if (root.has("is_smell") && !root.get("is_smell").isJsonNull()) {
                r.isSmell = root.get("is_smell").getAsBoolean();
            }
            if (root.has("smells_detected") && root.get("smells_detected").isJsonArray()) {
                for (var el : root.getAsJsonArray("smells_detected")) {
                    if (!el.isJsonNull()) r.smells.add(el.getAsString());
                }
            }

        } catch (Exception ignore) {
            // mantém defaults
        }

        return r;
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
=======
    }
    
    /**
     * Ponto de entrada para o processo de detecção. É chamado quando o botão "Detectar" é clicado.
     */
    private void executeDetectionProcess() {
        if (!executeButton.isEnabled()) {
            MessageDialog.openError(getSite().getShell(), "Erro", "Todos os arquivos devem ser carregados.");
            return;
        }
        
        try {
            String metricValuesFile = filePaths[1];
            loadDataFromCSV(metricValuesFile);

            String badSmellResult = "3 badsmells detectados";
            String status = "Sucesso";
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            String scriptName = new File(filePaths[0]).getName();
            
            ExecutionLogEntry logEntry = new ExecutionLogEntry(timestamp, scriptName, badSmellResult, status);
            addLogToHistory(logEntry);

            MessageDialog.openInformation(
                getSite().getShell(),
                "Processo Concluído",
                "Detecção de badsmells finalizada com sucesso!"
            );

        } catch (Exception e) {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            String scriptName = new File(filePaths[0]).getName();
            ExecutionLogEntry logEntry = new ExecutionLogEntry(timestamp, scriptName, "N/A", "Falha");
            addLogToHistory(logEntry);
            MessageDialog.openError(getSite().getShell(), "Erro", "Falha ao processar arquivos: " + e.getMessage());
        }
    }
    
    /**
>>>>>>> 23283ea (feat: Adiciona histórico de execuções e refatora a interface da view)
     * Cria a tabela para exibir as métricas e suas classificações.
     * @param parent O composite pai.
     */
    private void createMetricsTable(Composite parent) {
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
        historyGroup.setText("Histórico de Execuções");
        historyGroup.setLayout(new GridLayout(1, false));
        historyGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        historyGroup.setFont(new Font(parent.getDisplay(), "Segoe UI", 9, SWT.BOLD));

        historyTable = new Table(historyGroup, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
        historyTable.setHeaderVisible(true);
        historyTable.setLinesVisible(true);
        historyTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
<<<<<<< HEAD

        String[] headers = {"Data - Hora", "Script", "Badsmells", "Status"};
        for (String header : headers) {
            TableColumn column = new TableColumn(historyTable, SWT.NONE);
            column.setText(header);
        }

        Button clearButton = new Button(historyGroup, SWT.PUSH);
        clearButton.setText("Limpar Histórico");
        clearButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        clearButton.addListener(SWT.Selection, e -> clearHistory());
        
        for (TableColumn col : historyTable.getColumns()) {
            col.pack();
        }
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
            br.readLine(); // Pula a linha do cabeçalho
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    String metricName = parts[0].trim();
                    double lowThreshold = Double.parseDouble(parts[1].trim());
                    double mediumThreshold = Double.parseDouble(parts[2].trim());
                    metricThresholds.put(metricName, new double[]{lowThreshold, mediumThreshold});
                }
            }
=======

        String[] headers = {"Data - Hora", "Script", "Badsmells", "Status"};
        for (String header : headers) {
            TableColumn column = new TableColumn(historyTable, SWT.NONE);
            column.setText(header);
        }

        Button clearButton = new Button(historyGroup, SWT.PUSH);
        clearButton.setText("Limpar Histórico");
        clearButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        clearButton.addListener(SWT.Selection, e -> clearHistory());
        
        for (TableColumn col : historyTable.getColumns()) {
            col.pack();
        }
    }
    
    /**
     * Lê os dados de métricas de um arquivo CSV, processa e gera a tabela e o gráfico.
     * @param filePath O caminho para o arquivo CSV.
     */
    private void loadDataFromCSV(String filePath) {
        clearMetricsAndChart();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line = br.readLine();
            if (line == null) throw new IOException("Arquivo vazio");

            String[] values = line.split(",");
            if (values.length != metricas.length)
                throw new IOException("Esperado " + metricas.length + " valores de métricas");

            double[] yValues = processMetrics(values);
            generateChart(yValues);

        } catch (Exception e) {
            throw new RuntimeException(e);
>>>>>>> 23283ea (feat: Adiciona histórico de execuções e refatora a interface da view)
        }
    }

    /**
<<<<<<< HEAD
     * Lê os dados de métricas de um arquivo CSV, processa e então gera a tabela e o gráfico.
     * O formato esperado é: Metrica,Valor
     * @param filePath O caminho para o arquivo CSV de valores.
     */
    private void loadMetricValuesAndProcess(String filePath) throws IOException, NumberFormatException {
        metricValues = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            br.readLine(); // Pula a linha do cabeçalho
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    String metricName = parts[0].trim();
                    double value = Double.parseDouble(parts[1].trim());
                    metricValues.put(metricName, value);
                }
            }
        }
        
        // Após carregar os dados, limpa a UI e processa os novos valores
        clearMetricsAndChart();
        processMetrics();
    }

    /**
=======
>>>>>>> 23283ea (feat: Adiciona histórico de execuções e refatora a interface da view)
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
<<<<<<< HEAD
     */
    private void processMetrics() {
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
        for (TableColumn col : metricsTable.getColumns()) col.pack();
        
        generateChart(sortedMetrics.toArray(new String[0]), numericClassifications);
=======
     * @param valores Array de strings com os valores das métricas.
     * @return Array de doubles com os valores numéricos para o eixo Y do gráfico.
     */
    private double[] processMetrics(String[] valores) {
        double[] numericValues = new double[metricas.length];
        for (int i = 0; i < valores.length; i++) {
            String valueStr = valores[i].trim();
            double value = Double.parseDouble(valueStr);
            String classification = classify(metricas[i], value);
            addMetricTableRow(metricas[i], valueStr, classification);
            numericValues[i] = switch (classification) {
                case "LOW" -> 1.0;
                case "MEDIUM" -> 2.0;
                case "HIGH" -> 3.0;
                default -> 0.0;
            };
        }
        for (TableColumn col : metricsTable.getColumns()) col.pack();
        return numericValues;
>>>>>>> 23283ea (feat: Adiciona histórico de execuções e refatora a interface da view)
    }

    /**
     * Adiciona uma nova linha à tabela de métricas.
     * @param metrica O nome da métrica.
     * @param valor O valor da métrica.
     * @param classificacao A classificação (LOW, MEDIUM, HIGH).
     */
    private void addMetricTableRow(String metrica, String valor, String classificacao) {
        TableItem item = new TableItem(metricsTable, SWT.NONE);
        item.setText(new String[]{metrica, valor, classificacao});
        item.setForeground(fontColor);
    }

    /**
     * Gera e estiliza o gráfico de barras com base nos valores das métricas.
<<<<<<< HEAD
     * @param metricNames Os nomes das métricas para o eixo X.
     * @param valoresNumericos Array de dados para a série.
     */
    private void generateChart(String[] metricNames, double[] valoresNumericos) {
=======
     * @param valoresNumericos Os valores para o eixo Y do gráfico.
     */
    @SuppressWarnings("deprecation")
	private void generateChart(double[] valoresNumericos) {
>>>>>>> 23283ea (feat: Adiciona histórico de execuções e refatora a interface da view)
        Display display = chartComposite.getDisplay();
        Chart chart = new Chart(chartComposite, SWT.NONE);
        
        // --- Estilização do Gráfico ---
        chart.setBackground(lightGrayBackground);
        chart.getTitle().setText("Classificação das Métricas");
        chart.getTitle().setFont(new Font(display, "Segoe UI", 11, SWT.BOLD));
        chart.getTitle().setForeground(fontColor);

        IAxisSet axisSet = chart.getAxisSet();
        
        // --- Eixo Y (Valores) ---
        IAxis yAxis = axisSet.getYAxis(0);
<<<<<<< HEAD
        yAxis.getTitle().setText("Classificação de Risco (1=Baixo, 2=Médio, 3=Alto)"); 
=======
        yAxis.getTitle().setText("Classificação (1=Low, 2=Medium, 3=High)"); 
>>>>>>> 23283ea (feat: Adiciona histórico de execuções e refatora a interface da view)
        yAxis.getTitle().setFont(new Font(display, "Segoe UI", 9, SWT.NORMAL));
        yAxis.getTitle().setForeground(fontColor);
        yAxis.getTick().setFont(new Font(display, "Segoe UI", 8, SWT.NORMAL));
        yAxis.getTick().setForeground(fontColor);
        yAxis.getGrid().setForeground(new Color(display, 224, 224, 224));
<<<<<<< HEAD
        yAxis.setRange(new Range(0, 4));

=======
        
>>>>>>> 23283ea (feat: Adiciona histórico de execuções e refatora a interface da view)
        // --- Eixo X (Categorias) ---
        IAxis xAxis = axisSet.getXAxis(0);
        xAxis.getTitle().setText("Métricas");
        xAxis.getTitle().setFont(new Font(display, "Segoe UI", 9, SWT.NORMAL));
        xAxis.getTitle().setForeground(fontColor);
        xAxis.getTick().setFont(new Font(display, "Segoe UI", 8, SWT.NORMAL));
        xAxis.getTick().setForeground(fontColor);
<<<<<<< HEAD
        xAxis.setCategorySeries(metricNames);
        xAxis.enableCategory(true);
        xAxis.getGrid().setVisible(false);

        // --- Séries de Dados ---
=======
        xAxis.setCategorySeries(metricas);
        xAxis.getGrid().setVisible(false);

        // --- Séries de Dados ---
        double[] xSeries = IntStream.range(0, metricas.length).mapToDouble(i -> (double) i).toArray();
>>>>>>> 23283ea (feat: Adiciona histórico de execuções e refatora a interface da view)
        IBarSeries series = (IBarSeries) chart.getSeriesSet().createSeries(SeriesType.BAR, "Classificação");
        series.setYSeries(valoresNumericos);
        series.setBarColor(primaryBlue); // Todas as barras terão a mesma cor

        chart.getLegend().setPosition(SWT.RIGHT);

        axisSet.adjustRange();
        chartComposite.layout();
    }

    /**
<<<<<<< HEAD
     * Classifica um valor de métrica como LOW, MEDIUM ou HIGH com base nos limiares carregados.
=======
     * Classifica um valor de métrica como LOW, MEDIUM ou HIGH com base em limiares pré-definidos.
>>>>>>> 23283ea (feat: Adiciona histórico de execuções e refatora a interface da view)
     * @param metrica O nome da métrica.
     * @param valor O valor a ser classificado.
     * @return A string de classificação.
     */
    private String classify(String metrica, double valor) {
<<<<<<< HEAD
        if (metricThresholds == null || !metricThresholds.containsKey(metrica)) {
            return "UNKNOWN";
        }
        
        double[] thresholds = metricThresholds.get(metrica); // thresholds[0] = limite LOW, thresholds[1] = limite MEDIUM
        
        if (valor <= thresholds[0]) {
            return "LOW";
        } else if (valor <= thresholds[1]) {
            return "MEDIUM";
        } else {
            return "HIGH";
        }
=======
        return switch (metrica) {
            case "LOC" -> valor < 100 ? "LOW" : valor < 300 ? "MEDIUM" : "HIGH";
            case "NOA" -> valor < 5 ? "LOW" : valor < 10 ? "MEDIUM" : "HIGH";
            case "NOM" -> valor < 5 ? "LOW" : valor < 15 ? "MEDIUM" : "HIGH";
            case "WMC" -> valor < 10 ? "LOW" : valor < 30 ? "MEDIUM" : "HIGH";
            case "LCOM" -> valor < 0.3 ? "LOW" : valor < 0.7 ? "MEDIUM" : "HIGH";
            case "CBO" -> valor < 5 ? "LOW" : valor < 15 ? "MEDIUM" : "HIGH";
            case "DIT" -> valor < 2 ? "LOW" : valor < 5 ? "MEDIUM" : "HIGH";
            case "NOC" -> valor == 0 ? "LOW" : valor < 5 ? "MEDIUM" : "HIGH";
            default -> "UNKNOWN";
        };
>>>>>>> 23283ea (feat: Adiciona histórico de execuções e refatora a interface da view)
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
            System.err.println("Erro ao carregar histórico: " + e.getMessage());
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
             System.err.println("Erro ao salvar histórico: " + e.getMessage());
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
            "Confirmar Limpeza",
            "Você tem certeza que deseja limpar todo o histórico de execuções? Esta ação não pode ser desfeita."
        );

        if (confirmed) {
            historyTable.removeAll();
            try {
                Files.deleteIfExists(Paths.get(getHistoryFilePath()));
            } catch (IOException e) {
                MessageDialog.openError(getSite().getShell(), "Erro", "Não foi possível deletar o arquivo de histórico.");
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
            Map<String, double[]> thresholdsLoaded
    ) throws IOException {
        Gson gson = new Gson();

        String ctx = "CTX-" + System.currentTimeMillis();

        Header header = new Header();
        header.codCompany = "001";
        header.codContext = ctx;
        header.companyName = "EclipsePlugin";
        header.name = "SmellDSL Analysis";
        header.smell_type = "Bloaters";
        header.is_smell = false;
        header.timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(new Date());

        // ✅ extrai o smell do DSL e monta env
        String smellName = extractPrimarySmellName(dslCode);
        Map<String, Object> env = buildEnvFromMetricsAndThresholds(metricsLoaded, thresholdsLoaded, smellName);

        AnalyzeRequest request = new AnalyzeRequest();
        request.id = ctx;
        request.header = header;
        request.file_content = dslCode;
        request.smell_dsl = dslCode;
        request.env = env;


        String jsonBody = gson.toJson(request);

        HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        InputStream is = status < 300 ? conn.getInputStream() : conn.getErrorStream();
        String responseJson = new BufferedReader(new InputStreamReader(is))
                .lines().collect(Collectors.joining("\n"));

        if (status >= 300) {
            throw new IOException("Erro HTTP " + status + " em /analyze: " + responseJson);
        }

        AnalyzeResponse parsed = gson.fromJson(responseJson, AnalyzeResponse.class);
        if (parsed == null || parsed.execution_id == null || parsed.execution_id.isBlank()) {
            throw new IOException("Resposta inválida do /analyze: " + responseJson);
        }
        return parsed;
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
	    String execution_id;   // <-- NOVO
	    String id;
	    String status;
	    Result result;
	}

	private static class Result {
	    boolean interpreted;
	    List<String> smells;
	    Object rules;
	    Object treatments;
	}



}
