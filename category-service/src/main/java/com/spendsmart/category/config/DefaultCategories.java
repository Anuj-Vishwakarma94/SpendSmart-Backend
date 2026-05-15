package com.spendsmart.category.config;

import com.spendsmart.category.entity.Category;
import lombok.Getter;

import java.util.List;

/**
 * Defines the default (system) categories pre-seeded for every new user.
 * These are created once at application start with isDefault = true and userId = null.
 */
public class DefaultCategories {

    @Getter
    public static class DefaultCategoryDef {
        private final String name;
        private final Category.CategoryType type;
        private final String icon;
        private final String colorCode;

        public DefaultCategoryDef(String name, Category.CategoryType type, String icon, String colorCode) {
            this.name      = name;
            this.type      = type;
            this.icon      = icon;
            this.colorCode = colorCode;
        }
    }

    public static final List<DefaultCategoryDef> EXPENSE_DEFAULTS = List.of(
        new DefaultCategoryDef("Food & Dining",    Category.CategoryType.EXPENSE, "🍔", "#f85149"),
        new DefaultCategoryDef("Transport",        Category.CategoryType.EXPENSE, "🚗", "#58a6ff"),
        new DefaultCategoryDef("Shopping",         Category.CategoryType.EXPENSE, "🛍️", "#bc8cff"),
        new DefaultCategoryDef("Bills & Utilities",Category.CategoryType.EXPENSE, "⚡", "#d29922"),
        new DefaultCategoryDef("Health",           Category.CategoryType.EXPENSE, "💊", "#3fb950"),
        new DefaultCategoryDef("Entertainment",    Category.CategoryType.EXPENSE, "🎬", "#ff7b72"),
        new DefaultCategoryDef("Education",        Category.CategoryType.EXPENSE, "📚", "#79c0ff"),
        new DefaultCategoryDef("Travel",           Category.CategoryType.EXPENSE, "✈️", "#56d364"),
        new DefaultCategoryDef("Groceries",        Category.CategoryType.EXPENSE, "🛒", "#e3b341"),
        new DefaultCategoryDef("Rent",             Category.CategoryType.EXPENSE, "🏠", "#f0883e"),
        new DefaultCategoryDef("Subscriptions",    Category.CategoryType.EXPENSE, "📱", "#a5d6ff"),
        new DefaultCategoryDef("Other",            Category.CategoryType.EXPENSE, "📦", "#8b949e")
    );

    public static final List<DefaultCategoryDef> INCOME_DEFAULTS = List.of(
        new DefaultCategoryDef("Salary",           Category.CategoryType.INCOME, "💼", "#3fb950"),
        new DefaultCategoryDef("Freelance",        Category.CategoryType.INCOME, "🧑‍💻", "#58a6ff"),
        new DefaultCategoryDef("Business",         Category.CategoryType.INCOME, "🏢", "#bc8cff"),
        new DefaultCategoryDef("Investment",       Category.CategoryType.INCOME, "📈", "#d29922"),
        new DefaultCategoryDef("Gift",             Category.CategoryType.INCOME, "🎁", "#ff7b72"),
        new DefaultCategoryDef("Other Income",     Category.CategoryType.INCOME, "📦", "#8b949e")
    );
}
