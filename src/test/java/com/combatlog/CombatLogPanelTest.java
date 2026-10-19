package com.combatlog;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CombatLogPanelTest
{
	@Test
	public void panelPaintsDarkAndHasNoTextArea() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			CombatLogConfig config = (CombatLogConfig) Proxy.newProxyInstance(
				CombatLogConfig.class.getClassLoader(),
				new Class[]{CombatLogConfig.class},
				(proxy, method, args) ->
				{
					if (method.getReturnType() == boolean.class)
					{
						return Boolean.TRUE;
					}
					if (method.getReturnType() == int.class)
					{
						return Integer.valueOf(10);
					}
					if (method.getReturnType() == String.class)
					{
						return "";
					}
					return null;
				});
			CombatLog log = new CombatLog(20);
			CombatLogPanel panel = new CombatLogPanel(log, config, new MonsterIcons(), new WeaponIcons(null));
			log.add(new HitRecord(1_000L, "Guard", TargetKind.NPC, null, 1, null, 12, HitKind.HIT, null));
			log.add(new HitRecord(2_000L, "Guard", TargetKind.NPC, null, 2, null, 8, HitKind.HIT, null));
			panel.refresh();

			assertFalse(contains(panel.getWrappedPanel(), JTextComponent.class));
			assertFalse(contains(panel.getWrappedPanel(), JTextArea.class));
			assertEquals(PluginPanel.PANEL_WIDTH, panel.getPreferredSize().width);
			assertTrue(panel.getPreferredSize().height > 0);

			JScrollPane scrollPane = scrollPane(panel);
			JViewport viewport = scrollPane.getViewport();
			assertEquals(JViewport.SIMPLE_SCROLL_MODE, viewport.getScrollMode());
			assertEquals(ColorScheme.DARK_GRAY_COLOR, viewport.getBackground());
			assertTrue(viewport.isOpaque());
			assertEquals(ColorScheme.DARK_GRAY_COLOR, panel.getBackground());

			JFrame frame = new JFrame();
			try
			{
				frame.getContentPane().add(panel.getWrappedPanel());
				frame.setSize(PluginPanel.PANEL_WIDTH + PluginPanel.SCROLLBAR_WIDTH + 20, 640);
				frame.setVisible(true);
				frame.validate();
				panel.refresh();
				frame.validate();

				java.awt.Container content = frame.getContentPane();
				BufferedImage image = new BufferedImage(content.getWidth(), content.getHeight(), BufferedImage.TYPE_INT_RGB);
				Graphics2D graphics = image.createGraphics();
				graphics.setColor(Color.WHITE);
				graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
				content.paint(graphics);
				graphics.dispose();

				int background = image.getRGB(24, image.getHeight() - 24);
				assertTrue("empty sidebar area stayed white: " + Integer.toHexString(background),
					isDark(background));
				int mid = image.getRGB(24, image.getHeight() / 2);
				assertTrue("panel surface stayed white: " + Integer.toHexString(mid), isDark(mid));
			}
			finally
			{
				frame.dispose();
			}
		});
	}

	private static boolean isDark(int rgb)
	{
		int red = (rgb >> 16) & 255;
		int green = (rgb >> 8) & 255;
		int blue = rgb & 255;
		return red < 80 && green < 80 && blue < 80;
	}

	private static JScrollPane scrollPane(CombatLogPanel panel)
	{
		try
		{
			java.lang.reflect.Method method = PluginPanel.class.getDeclaredMethod("getScrollPane");
			method.setAccessible(true);
			return (JScrollPane) method.invoke(panel);
		}
		catch (ReflectiveOperationException e)
		{
			throw new AssertionError(e);
		}
	}

	private static boolean contains(Component component, Class<?> type)
	{
		if (type.isInstance(component))
		{
			return true;
		}
		if (component instanceof java.awt.Container)
		{
			java.awt.Container container = (java.awt.Container) component;
			for (int i = 0; i < container.getComponentCount(); i++)
			{
				if (contains(container.getComponent(i), type))
				{
					return true;
				}
			}
		}
		return false;
	}
}
