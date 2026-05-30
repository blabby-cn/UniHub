package com.Blabby.Co.UniHub.ui.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.Blabby.Co.UniHub.R;
import com.Blabby.Co.UniHub.util.Localization;

public class LoginDialog extends DialogFragment {
    public interface LoginListener {
        void onLogin(String host, int port, String user, String pass);
    }

    private final LoginListener listener;
    private String preHost = "";
    private int prePort = 21;

    public LoginDialog(String host, int port, LoginListener listener) {
        this.preHost = host;
        this.prePort = port;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity(), R.style.AlertDialogTheme);
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_login, null);

        EditText etHost = view.findViewById(R.id.et_host);
        EditText etPort = view.findViewById(R.id.et_port);
        EditText etUser = view.findViewById(R.id.et_username);
        EditText etPass = view.findViewById(R.id.et_password);

        etHost.setText(preHost);
        etPort.setText(String.valueOf(prePort));

        Localization l = Localization.getInstance(requireContext());
        builder.setView(view)
                .setTitle(l.get("login_remote_server"))
                .setPositiveButton(l.get("connect"), (dialog, which) -> {
                    String host = etHost.getText().toString().trim();
                    String portStr = etPort.getText().toString().trim();
                    String user = etUser.getText().toString().trim();
                    String pass = etPass.getText().toString().trim();
                    if (host.isEmpty()) {
                        Toast.makeText(requireContext(), l.get("name_empty"), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int port;
                    try {
                        port = Integer.parseInt(portStr);
                    } catch (NumberFormatException e) {
                        Toast.makeText(requireContext(), l.get("error_colon", l.get("port_hint")), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (listener != null) listener.onLogin(host, port, user, pass);
                })
                .setNegativeButton(l.get("cancel"), null);
        return builder.create();
    }
}