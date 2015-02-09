package tv.inair.airmote;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * <p>
 * Note this class is currently under early design and development.
 * The API will likely change in later updates of the compatibility library,
 * requiring changes to the source code of apps when they are compiled against the newer version.
 * </p>
 * <p/>
 * <p>Copyright (c) 2014 SeeSpace.co. All rights reserved.</p>
 */
public class DeviceAdapter extends ArrayAdapter<NsdHelper.Item> {
  final Handler mHandler = new Handler();

  public class ViewHolder {
    public TextView nameView;
    public TextView addrView;
  }

  public DeviceAdapter(Context context) {
    super(context, R.layout.device_item);
  }

  public void addItem(final NsdHelper.Item device) {
    mHandler.post(
        new Runnable() {
          @Override
          public void run() {
            add(device);
          }
        }
    );
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    View rowView = convertView;
    // reuse views
    if (rowView == null) {
      LayoutInflater inflater = ((Activity) getContext()).getLayoutInflater();
      rowView = inflater.inflate(R.layout.device_item, null);
      // configure view holder
      ViewHolder viewHolder = new ViewHolder();
      viewHolder.nameView = (TextView) rowView.findViewById(R.id.displayName);
      viewHolder.addrView = (TextView) rowView.findViewById(R.id.hostName);
      rowView.setTag(viewHolder);
    }

    // fill data
    ViewHolder holder = (ViewHolder) rowView.getTag();
    NsdHelper.Item item = getItem(position);
    holder.nameView.setText(item.mDisplayName);
    holder.addrView.setText(item.mHostName);

    return rowView;
  }
}
