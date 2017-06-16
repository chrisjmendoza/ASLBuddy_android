package xyz.softdev.aslbuddy;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class HohMenu extends Activity {

    int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hoh_menu);

        userId = getIntent().getIntExtra("userId", userId);

        Button physReq = (Button)findViewById(R.id.physReq);
        physReq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(HohMenu.this, PhysicalRequestForm.class);
                HohMenu.this.startActivity(i);
            }
        });

        Button skypeButton = (Button) findViewById(R.id.skypeCall);
        skypeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(HohMenu.this, SkypeCall.class);
                HohMenu.this.startActivity(i);
            }
        });

        Button chatButton = (Button) findViewById(R.id.chatTool);
        chatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(HohMenu.this, ChatTool.class);
                i.putExtra("userId", userId);
                HohMenu.this.startActivity(i);
            }
        });

    }
}
