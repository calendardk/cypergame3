package com.cybergame.ui.fxcontroller;

import com.cybergame.controller.AuthController;
import com.cybergame.controller.EmployeeAuthController;
import com.cybergame.controller.SessionManager;
import com.cybergame.model.entity.Computer;
import com.cybergame.model.entity.Employee;
import com.cybergame.model.entity.Session;
import com.cybergame.repository.sql.*;
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

public class LoginController {

    // --- DEPENDENCIES (STAFF/ADMIN) ---
    private final EmployeeRepositorySQL empRepo = new EmployeeRepositorySQL();
    private final EmployeeAuthController empAuth = new EmployeeAuthController(empRepo);

    // --- DEPENDENCIES (CLIENT) ---
    private final AccountRepositorySQL accRepo = new AccountRepositorySQL();
    private final SessionRepositorySQL sessionRepo = new SessionRepositorySQL();
    private final InvoiceRepositorySQL invoiceRepo = new InvoiceRepositorySQL();
    private final ComputerRepositorySQL compRepo = new ComputerRepositorySQL();
    private final SessionManager sessionManager = new SessionManager(sessionRepo, invoiceRepo, accRepo);
    private final AuthController clientAuth = new AuthController(accRepo, sessionManager);

    // --- FXML FIELDS ---
    @FXML private TextField txtStaffUser;
    @FXML private PasswordField txtStaffPass;
    @FXML private PasswordField txtAdminPin;
    @FXML private TextField custUser;
    @FXML private PasswordField custPass;


    // =======================================================
    // 1. XỬ LÝ ĐĂNG NHẬP NHÂN VIÊN
    // =======================================================
    @FXML
    private void handleStaffLogin(ActionEvent event) {
        String user = txtStaffUser.getText();
        String pass = txtStaffPass.getText();

        if (user == null || user.isEmpty() || pass == null || pass.isEmpty()) {
            showAlert("Lỗi", "Vui lòng nhập tài khoản và mật khẩu!");
            return;
        }

        // 1. Kiểm tra DB
        Employee emp = empAuth.login(user, pass);

        if (emp != null) {
            System.out.println("Đăng nhập thành công: " + emp.getUsername());
            
            // 2. Chuyển cảnh và TRUYỀN DỮ LIỆU
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/staff/staff_dashboard.fxml"));
                Parent root = loader.load();

                // --- [QUAN TRỌNG] Lấy controller màn hình sau để set info ---
                StaffDashboardController dashboardCtrl = loader.getController();
                dashboardCtrl.setStaffInfo(emp); 
                // -------------------------------------------------------------

                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("CyberGame - Staff Dashboard");
                stage.centerOnScreen();
                stage.show();

            } catch (IOException e) {
                e.printStackTrace();
                showAlert("Lỗi Giao Diện", "Không thể tải Dashboard: " + e.getMessage());
            }

        } else {
            showAlert("Đăng nhập thất bại", "Sai thông tin hoặc tài khoản đã bị khóa!");
        }
    }

    // =======================================================
    // 2. XỬ LÝ ĐĂNG NHẬP ADMIN
    // =======================================================
    @FXML
    private void handleAdminLogin(ActionEvent event) {
        String pin = txtAdminPin.getText();

        if ("9999".equals(pin) || "admin".equals(pin)) {
            System.out.println("Admin đã đăng nhập bằng PIN.");
            navigate(event, "/fxml/admin/admin_dashboard.fxml", "CyberGame - Administrator");
        } else {
            showAlert("Truy cập bị từ chối", "Mã PIN xác thực không đúng!");
        }
    }

    // =======================================================
    // 3. XỬ LÝ ĐĂNG NHẬP KHÁCH HÀNG
    // =======================================================
    @FXML
    private void handleCustomerLogin(ActionEvent event) {
        String user = custUser.getText();
        String pass = custPass.getText();

        if (user.isEmpty() || pass.isEmpty()) {
            showAlert("Lỗi", "Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        int currentPcId = 1;
        Optional<Computer> pcOpt = compRepo.findAll().stream()
                .filter(c -> c.getComputerId() == currentPcId)
                .findFirst();

        if (pcOpt.isEmpty()) {
            showAlert("Lỗi", "Không tìm thấy thông tin máy trạm này!");
            return;
        }

        try {
            Session session = clientAuth.loginCustomer(user, pass, pcOpt.get());
            if (session != null) {
                navigate(event, "/fxml/client/client_dashboard.fxml", "CyberGame - Client Terminal");
            } else {
                showAlert("Thất bại", "Sai tài khoản/mật khẩu hoặc tài khoản hết tiền!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Lỗi hệ thống", e.getMessage());
        }
    }

    // =======================================================
    // UTILS
    // =======================================================
    private void navigate(ActionEvent event, String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Lỗi File Giao Diện", "Không tìm thấy file: " + fxmlPath + "\nChi tiết: " + e.getMessage());
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}