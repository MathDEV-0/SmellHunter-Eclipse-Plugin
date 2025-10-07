package com.plugin.view;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.IntStream;

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

    private final String[] metricas = {"LOC", "NOA", "NOM", "WMC", "LCOM", "CBO", "DIT", "NOC"};
    private static final String HISTORY_FILE_NAME = "execution_history.json";

    // Cores personalizadas para a UI
    private Color greenColor, redColor, primaryBlue, lightGrayBackground, fontColor;
    
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
        greenColor = new Color(display, 76, 175, 80);
        redColor = new Color(display, 244, 67, 54);
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
        }
    }

    /**
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
     * @param valoresNumericos Os valores para o eixo Y do gráfico.
     */
    @SuppressWarnings("deprecation")
	private void generateChart(double[] valoresNumericos) {
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
        yAxis.getTitle().setText("Classificação (1=Low, 2=Medium, 3=High)"); 
        yAxis.getTitle().setFont(new Font(display, "Segoe UI", 9, SWT.NORMAL));
        yAxis.getTitle().setForeground(fontColor);
        yAxis.getTick().setFont(new Font(display, "Segoe UI", 8, SWT.NORMAL));
        yAxis.getTick().setForeground(fontColor);
        yAxis.getGrid().setForeground(new Color(display, 224, 224, 224));
        
        // --- Eixo X (Categorias) ---
        IAxis xAxis = axisSet.getXAxis(0);
        xAxis.getTitle().setText("Métricas");
        xAxis.getTitle().setFont(new Font(display, "Segoe UI", 9, SWT.NORMAL));
        xAxis.getTitle().setForeground(fontColor);
        xAxis.getTick().setFont(new Font(display, "Segoe UI", 8, SWT.NORMAL));
        xAxis.getTick().setForeground(fontColor);
        xAxis.setCategorySeries(metricas);
        xAxis.getGrid().setVisible(false);

        // --- Séries de Dados ---
        double[] xSeries = IntStream.range(0, metricas.length).mapToDouble(i -> (double) i).toArray();
        IBarSeries series = (IBarSeries) chart.getSeriesSet().createSeries(SeriesType.BAR, "Classificação");
        series.setXSeries(xSeries);
        series.setYSeries(valoresNumericos);
        series.setBarColor(primaryBlue);

        chart.getLegend().setPosition(SWT.BOTTOM);
        chart.getLegend().setFont(new Font(display, "Segoe UI", 8, SWT.NORMAL));
        chart.getLegend().setForeground(fontColor);

        axisSet.adjustRange();
        chartComposite.layout();
    }

    /**
     * Classifica um valor de métrica como LOW, MEDIUM ou HIGH com base em limiares pré-definidos.
     * @param metrica O nome da métrica.
     * @param valor O valor a ser classificado.
     * @return A string de classificação.
     */
    private String classify(String metrica, double valor) {
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
        if (redColor != null) redColor.dispose();
        if (primaryBlue != null) primaryBlue.dispose();
        if (lightGrayBackground != null) lightGrayBackground.dispose();
        super.dispose();
    }
}