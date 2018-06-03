package de.westnordost.streetcomplete.quests.max_speed;


import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import de.westnordost.streetcomplete.R;
import de.westnordost.streetcomplete.quests.max_speed.model.MaxSpeed;
import de.westnordost.streetcomplete.quests.opening_hours.adapter.AddOpeningHoursAdapter;
import de.westnordost.streetcomplete.view.Item;
import de.westnordost.streetcomplete.view.dialogs.AlertDialogBuilder;

public class SpeedInputDialog extends AlertDialog implements DialogInterface.OnClickListener{
	protected String maxSpeedType;

	protected SpeedInputDialog(@NonNull Context context, Item viewType, final EditText.ON) {
		super(context);
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {

	}

	public interface OnSpeedInputListener
	{
		void onSpeedInput(MaxSpeed maxspeed);
		void onCancel();
	}

	public static AlertDialog show(Context context, Item viewType, final OnSpeedInputListener callback) {

		LayoutInflater inflater = LayoutInflater.from(context);
		View v;
		switch (viewType.value) {
			case "maxspeed":
				v = inflater.inflate(R.layout.quest_maxspeed, this);
				return new AddOpeningHoursAdapter.MonthsViewHolder(
					inflater.inflate(R.layout.quest_times_month_row, parent, false));
		}

		AlertDialog dlg = new AlertDialogBuilder(context)
			.setTitle(R.string.quest_maxspeed_name_explicit)
			.setView()
			.setNegativeButton(android.R.string.cancel,
				(dialog, which) -> callback.onCancel())
			.setPositiveButton(android.R.string.ok,
				(dialog, which) -> callback.onSpeedInput(new MaxSpeed()))
			.show();
		updateDialogOkButtonEnablement(dlg);
		return dlg;
	}

	private static void updateDialogOkButtonEnablement(android.support.v7.app.AlertDialog dlg, boolean[] selection)
	{
		boolean isAnyChecked = false;
		for(boolean b : selection) isAnyChecked |= b;
		dlg.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(isAnyChecked);
	}
}
