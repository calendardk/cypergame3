package com.cybergame.ui.fxcontroller;

import com.cybergame.controller.AuthController;
import com.cybergame.controller.SessionManager;
import com.cybergame.model.entity.Computer;
import com.cybergame.model.entity.Session;
import com.cybergame.repository.sql.AccountRepositorySQL;
import com.cybergame.repository.sql.ComputerRepositorySQL;
import com.cybergame.repository.sql.InvoiceRepositorySQL;
import com.cybergame.repository.sql.SessionRepositorySQL;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

public class ClientLoginController {

    // --- FXML FIELDS (Khớp với file client_login.fxml) ---
    @FXML private TextField custUser;
    @FXML private PasswordField custPass;

    // --- DEPENDENCIES ---
    private final AccountRepositorySQL accRepo = new AccountRepositorySQL();
    private final SessionRepositorySQL sessionRepo = new SessionRepositorySQL();
    private final InvoiceRepositorySQL invoiceRepo = new InvoiceRepositorySQL();
    private final ComputerRepositorySQL compRepo = new ComputerRepositorySQL();
    
    private final SessionManager sessionManager = new SessionManager(sessionRepo, invoiceRepo, accRepo);
    private final AuthController clientAuth = new AuthController(accRepo, sessionManager);

    // =======================================================
    // XỬ LÝ ĐĂNG NHẬP KHÁCH HÀNG
    // =======================================================
    @FXML
    private void handleCustomerLogin(ActionEvent event) {
        String user = custUser.getText();
        String pass = custPass.getText();

        if (user.isEmpty() || pass.isEmpty()) {
            showAlert("Lỗi", "Vui lòng nhập User ID và Password!");
            return;
        }

        // --- CẤU HÌNH MÁY TRẠM ---
        // Giả lập đây là máy PC-1. Trong thực tế sẽ đọc từ file config (VD: computer_id=5)
        int currentPcId = 1; 
        
        Optional<Computer> pcOpt = compRepo.findAll().stream()
                .filter(c -> c.getComputerId() == currentPcId)
                .findFirst();

        if (pcOpt.isEmpty()) {
            showAlert("Lỗi Hệ Thống", "Không tìm thấy thông tin máy trạm (ID=" + currentPcId + ")!");
            return;
        }

        try {
            // 1. Gọi AuthController tạo Session
            Session session = clientAuth.loginCustomer(user, pass, pcOpt.get());
            
            if (session != null) {
                System.out.println("Client Login Success: " + user + " at " + pcOpt.get().getName());
                
                // 2. Chuyển sang màn hình Dashboard Khách
                navigateToDashboard(event, session);
                
            } else {
                showAlert("Đăng nhập thất bại", "Sai tài khoản, mật khẩu hoặc tài khoản hết tiền/đang online!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Lỗi hệ thống", e.getMessage());
        }
    }

    // --- HÀM CHUYỂN CẢNH & TRUYỀN DỮ LIỆU ---
    private void navigateToDashboard(ActionEvent event, Session session) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/client/client_dashboard.fxml"));
            Parent root = loader.load();

            // Lấy Controller của màn hình sau và truyền Session
            ClientController clientCtrl = loader.getController();
            clientCtrl.setSession(session);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("CyberGame Client - " + session.getAccount().getUsername());
            
            // Client thường để chế độ Full Screen để khách tập trung chơi
            stage.setFullScreen(true); 
            stage.setFullScreenExitHint(""); 
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Lỗi Giao Diện", "Không thể tải Dashboard: " + e.getMessage());
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }
}