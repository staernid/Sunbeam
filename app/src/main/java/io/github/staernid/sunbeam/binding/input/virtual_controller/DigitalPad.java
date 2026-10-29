/**
 * Created by Karim Mreisi.
 */

package io.github.staernid.sunbeam.binding.input.virtual_controller;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;

import io.github.staernid.sunbeam.LimeLog;
import io.github.staernid.sunbeam.R;
import io.github.staernid.sunbeam.preferences.PreferenceConfiguration;

import java.util.ArrayList;
import java.util.List;

public class DigitalPad extends VirtualControllerElement {
    public final static int DIGITAL_PAD_DIRECTION_NO_DIRECTION = 0;
    int direction = DIGITAL_PAD_DIRECTION_NO_DIRECTION;
    public final static int DIGITAL_PAD_DIRECTION_LEFT = 1;
    public final static int DIGITAL_PAD_DIRECTION_UP = 2;
    public final static int DIGITAL_PAD_DIRECTION_RIGHT = 4;
    public final static int DIGITAL_PAD_DIRECTION_DOWN = 8;
    List<DigitalPadListener> listeners = new ArrayList<>();

    private static final int DPAD_MARGIN = 5;
    private final RectF rect = new RectF();

    private final Paint paint = new Paint();

    public DigitalPad(VirtualController controller, Context context) {
        super(controller, context, EID_DPAD);
    }

    public void addDigitalPadListener(DigitalPadListener listener) {
        listeners.add(listener);
    }

    @Override
    protected void onElementDraw(Canvas canvas) {
        renderer.drawDigitalPad(
                canvas,
                getWidth(),
                getHeight(),
                direction,
                opacityPercent,
                virtualController.getControllerMode(),
                enabled
        );
    }

    public Drawable rotateDrawable(Drawable vectorDrawable, float angle) {
        int width = vectorDrawable.getIntrinsicWidth();
        int height = vectorDrawable.getIntrinsicHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vectorDrawable.draw(canvas);

        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return new BitmapDrawable(getResources(), rotatedBitmap);
    }

    private void newDirectionCallback(int direction) {
        _DBG("direction: " + direction);

        // notify listeners
        for (DigitalPadListener listener : listeners) {
            listener.onDirectionChange(direction);
        }
    }

    @Override
    public boolean onElementTouchEvent(MotionEvent event) {
        // get masked (not specific to a pointer) action
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE: {
                direction = 0;

                if (event.getX() < getPercent(getWidth(), 33)) {
                    direction |= DIGITAL_PAD_DIRECTION_LEFT;
                }
                if (event.getX() > getPercent(getWidth(), 66)) {
                    direction |= DIGITAL_PAD_DIRECTION_RIGHT;
                }
                if (event.getY() > getPercent(getHeight(), 66)) {
                    direction |= DIGITAL_PAD_DIRECTION_DOWN;
                }
                if (event.getY() < getPercent(getHeight(), 33)) {
                    direction |= DIGITAL_PAD_DIRECTION_UP;
                }
                newDirectionCallback(direction);
                invalidate();

                return true;
            }
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                direction = 0;
                newDirectionCallback(direction);
                invalidate();

                return true;
            }
            default: {
            }
        }

        return true;
    }

    public interface DigitalPadListener {
        void onDirectionChange(int direction);
    }
}
