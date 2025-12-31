package com.cybergame.ui.fxcontroller;

import com.cybergame.controller.AuthController;
import com.cybergame.controller.SessionManager;
import com.cybergame.model.entity.Session;
import com.cybergame.repository.sql.AccountRepositorySQL;
import com.cybergame.repository.sql.InvoiceRepositorySQL;
import com.cybergame.repository.sql.SessionRepositorySQL;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class ClientController {

    // --- FXML ELEMENTS (Khớp với file client_dashboard.fxml) ---
    @FXML private Label lblUsername;
    @FXML private Label lblPcName;
    @FXML private Label lblBalance;
    @FXML private Label lblTimeUsage;

    // --- DATA ---
    private Session currentSession;
    private Timeline usageTimer;

    // --- DEPENDENCIES (Để xử lý logout) ---
    private final AccountRepositorySQL accRepo = new AccountRepositorySQL();
    private final SessionRepositorySQL sessionRepo = new SessionRepositorySQL();
    private final InvoiceRepositorySQL invoiceRepo = new InvoiceRepositorySQL();
    private final SessionManager sessionManager = new SessionManager(sessionRepo, invoiceRepo, accRepo);
    private final AuthController authController = new AuthController(accRepo, sessionManager);

    // --- [QUAN TRỌNG] HÀM NHẬN DỮ LIỆU TỪ LOGIN ---
    public void setSession(Session session) {
        this.currentSession = session;
        updateUI();
        startTimer();
    }

    private void updateUI() {
        if (currentSession != null) {
            // Cập nhật Tên User
            lblUsername.setText(currentSession.getAccount().getUsername());
            
            // Cập nhật Tên Máy
            if (currentSession.getComputer() != null) {
                lblPcName.setText(currentSession.getComputer().getName());
            }
            
            // Cập nhật Số dư
            double balance = currentSession.getAccount().getBalance();
            lblBalance.setText(String.format("%,.0f đ", balance));
        }
    }

    // --- ĐỒNG HỒ ĐẾM GIỜ CHƠI ---
    private void startTimer() {
        if (usageTimer != null) usageTimer.stop();

        usageTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (currentSession != null) {
                LocalDateTime start = currentSession.getStartTime();
                LocalDateTime now = LocalDateTime.now();
                
                long seconds = ChronoUnit.SECONDS.between(start, now);
                long h = seconds / 3600;
                long m = (seconds % 3600) / 60;
                long s = seconds % 60;

                lblTimeUsage.setText(String.format("%02d:%02d:%02d", h, m, s));
                
                // TODO: Nếu muốn tự động logout khi hết tiền thì thêm logic ở đây
            }
        }));
        usageTimer.setCycleCount(Timeline.INDEFINITE);
        usageTimer.play();
    }

    // ================= ACTIONS =================

    @FXML
    private void openServiceMenu() {
        // Mở menu gọi món (Sẽ làm sau)
        System.out.println("Mở menu gọi đồ...");
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText("Tính năng đang phát triển");
        alert.show();
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        if (currentSession == null) return;

        // 1. Gọi backend để đóng phiên, tính tiền
        authController.logout(currentSession);
        
        // 2. Dừng đồng hồ
        if (usageTimer != null) usageTimer.stop();

        // 3. Quay về màn hình Login (client_login.fxml)
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login/client_login.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setFullScreen(true); // Giữ chế độ Fullscreen cho màn hình khóa
            stage.show();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}