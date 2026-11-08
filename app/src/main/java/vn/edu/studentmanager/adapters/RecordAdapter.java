package vn.edu.studentmanager.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import vn.edu.studentmanager.databinding.ItemRecordBinding;
import java.util.*;
import java.util.function.Consumer;

public class RecordAdapter<T> extends RecyclerView.Adapter<RecordAdapter.Holder> {
    public interface Presenter<T> {
        String title(T row);
        String subtitle(T row);
        String status(T row);
        default int statusColor(T row) { return vn.edu.studentmanager.R.color.text_secondary; }
    }
    private final List<T> rows = new ArrayList<>();
    private final Presenter<T> presenter;
    private final Consumer<T> click;
    public RecordAdapter(Presenter<T> presenter, Consumer<T> click) { this.presenter = presenter; this.click = click; }
    public void submit(List<T> items) { rows.clear(); rows.addAll(items); notifyDataSetChanged(); }
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new Holder(ItemRecordBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }
    @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
        T row = rows.get(position); String title = presenter.title(row);
        holder.binding.title.setText(title); holder.binding.subtitle.setText(presenter.subtitle(row));
        holder.binding.status.setText(presenter.status(row));
        holder.binding.status.setTextColor(holder.itemView.getContext().getColor(presenter.statusColor(row)));
        holder.binding.initial.setText(initials(title));
        holder.itemView.setOnClickListener(v -> click.accept(row));
    }
    public static String initials(String name) {
        if (name == null || name.isBlank()) return "SV";
        String[] parts = name.trim().split("\\s+");
        return (parts.length == 1 ? parts[0].substring(0, 1) : parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase(Locale.ROOT);
    }
    @Override public int getItemCount() { return rows.size(); }
    static class Holder extends RecyclerView.ViewHolder {
        final ItemRecordBinding binding;
        Holder(ItemRecordBinding b) { super(b.getRoot()); binding = b; }
    }
}
