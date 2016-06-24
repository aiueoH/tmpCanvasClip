package rd.canvaslasso;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    private Button button;
    private MyCanvas myCanvas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myCanvas = (MyCanvas) findViewById(R.id.myCanvas);
        button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (myCanvas.getMode().equals(MyCanvas.Mode.Brush))
                    myCanvas.setMode(MyCanvas.Mode.Lasso);
                else
                    myCanvas.setMode(MyCanvas.Mode.Brush);
            }
        });
    }

}
